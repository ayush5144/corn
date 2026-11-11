// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

//! flush-store: Corn's local database (SQLite).
//!
//! Holds everything Corn keeps that could reveal browsing:
//! - the **Vault**: saved links (Vault Items), each stored once, optionally in one Collection.
//!   "All" is not stored: it is simply every saved link.
//! - **History**: one entry per page plus a visit log with timestamps (cleared by Flush).
//!   Deletion is all-or-nothing, per visit, or per site (history only): no time ranges.
//! - a small key-value area for the Keep List, per-site shields and the Kept Session.
//! - **Passwords** (`logins`): one row per saved login. The site (origin) is stored in clear so
//!   Corn can look logins up; the username and password are opaque blobs encrypted by the app
//!   (AES-256-GCM with a key protected by the phone's keystore). This crate never sees them in clear.
//!   Like the Vault, passwords survive every Flush.
//!
//! The file lives in the app's private storage, protected by the OS sandbox
//! and the phone's own encryption; the Vault lock gates access in the UI.

use std::sync::Mutex;
use std::time::{SystemTime, UNIX_EPOCH};

use rusqlite::{params, Connection, OptionalExtension};

/// Schema version written to `PRAGMA user_version`.
///
/// - 0: first release (every Vault Item in a Collection, "Unsorted" catch-all).
/// - 2: optional Collection ("All"), history.
/// - 3: `logins` (saved passwords).
const SCHEMA_VERSION: i64 = 3;

#[derive(Debug, thiserror::Error)]
pub enum VaultError {
    #[error("a collection named \"{0}\" already exists")]
    DuplicateCollection(String),
    #[error("collection names can't be empty")]
    EmptyName,
    #[error("database error: {0}")]
    Db(#[from] rusqlite::Error),
}

pub type Result<T> = std::result::Result<T, VaultError>;

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Collection {
    pub id: i64,
    pub name: String,
    pub item_count: i64,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct VaultItem {
    pub id: i64,
    /// None = in no Collection (shown only in All).
    pub collection_id: Option<i64>,
    pub title: String,
    pub url: String,
    pub tags: Vec<String>,
    pub created_at: i64,
}

/// One visit to a page, for the History screen.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct HistoryVisit {
    pub visit_id: i64,
    pub url: String,
    pub title: String,
    pub host: String,
    pub visited_at: i64,
}

/// One saved login. `username_enc` and `password_enc` are encrypted by the app; Rust never decrypts them.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Login {
    pub id: i64,
    /// `scheme://host[:port]`, e.g. `https://example.com` (port only if not the default).
    pub origin: String,
    pub username_enc: Vec<u8>,
    pub password_enc: Vec<u8>,
    pub created_at: i64,
    pub last_used_at: i64,
    pub times_used: i64,
}

pub struct Store {
    conn: Mutex<Connection>,
}

const SCHEMA: &str = "
CREATE TABLE IF NOT EXISTS collections (
    id         INTEGER PRIMARY KEY,
    name       TEXT NOT NULL UNIQUE COLLATE NOCASE,
    created_at INTEGER NOT NULL
);
CREATE TABLE IF NOT EXISTS items (
    id            INTEGER PRIMARY KEY,
    collection_id INTEGER REFERENCES collections(id) ON DELETE SET NULL,
    title         TEXT NOT NULL,
    url           TEXT NOT NULL,
    created_at    INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS items_by_collection ON items(collection_id, created_at DESC);
CREATE TABLE IF NOT EXISTS item_tags (
    item_id INTEGER NOT NULL REFERENCES items(id) ON DELETE CASCADE,
    tag     TEXT NOT NULL,
    PRIMARY KEY (item_id, tag)
);
CREATE TABLE IF NOT EXISTS kv (
    key   TEXT PRIMARY KEY,
    value TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS history_entries (
    id         INTEGER PRIMARY KEY,
    url        TEXT NOT NULL UNIQUE,
    host       TEXT NOT NULL,
    title      TEXT NOT NULL,
    last_visit INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS history_by_host ON history_entries(host);
CREATE TABLE IF NOT EXISTS history_visits (
    id         INTEGER PRIMARY KEY,
    entry_id   INTEGER NOT NULL REFERENCES history_entries(id) ON DELETE CASCADE,
    visited_at INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS visits_by_time ON history_visits(visited_at DESC);
CREATE TABLE IF NOT EXISTS logins (
    id           INTEGER PRIMARY KEY,
    origin       TEXT NOT NULL,
    username_enc BLOB NOT NULL,
    password_enc BLOB NOT NULL,
    created_at   INTEGER NOT NULL,
    last_used_at INTEGER NOT NULL,
    times_used   INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS logins_by_origin ON logins(origin);
";

const LOGIN_COLUMNS: &str = "id, origin, username_enc, password_enc, created_at, last_used_at, times_used";
/// Most recently used first, then most used.
const LOGIN_ORDER: &str = "ORDER BY last_used_at DESC, times_used DESC, id DESC";

fn login_row(r: &rusqlite::Row) -> rusqlite::Result<Login> {
    Ok(Login {
        id: r.get(0)?,
        origin: r.get(1)?,
        username_enc: r.get(2)?,
        password_enc: r.get(3)?,
        created_at: r.get(4)?,
        last_used_at: r.get(5)?,
        times_used: r.get(6)?,
    })
}

fn now() -> i64 {
    SystemTime::now().duration_since(UNIX_EPOCH).map(|d| d.as_millis() as i64).unwrap_or(0)
}

/// SQL condition matching a host and all its subdomains (`example.com`, `m.example.com`).
const HOST_MATCH: &str = "(host = ?1 OR host LIKE '%.' || ?1)";

impl Store {
    /// Open (or create, or upgrade) the database at `path`.
    pub fn open(path: &str) -> Result<Self> {
        let conn = Connection::open(path)?;
        // secure_delete: deleted rows are overwritten with zeros, not left in free space in the file.
        conn.execute_batch("PRAGMA foreign_keys = ON; PRAGMA journal_mode = WAL; PRAGMA secure_delete = ON;")?;
        migrate(&conn)?;
        Ok(Self { conn: Mutex::new(conn) })
    }

    fn conn(&self) -> std::sync::MutexGuard<'_, Connection> {
        self.conn.lock().unwrap_or_else(|e| e.into_inner())
    }

    // ---------- Collections ----------

    /// Every Collection with its item count, oldest first. "All" is not a Collection.
    pub fn collections(&self) -> Result<Vec<Collection>> {
        let conn = self.conn();
        let mut stmt = conn.prepare(
            "SELECT c.id, c.name, (SELECT count(*) FROM items i WHERE i.collection_id = c.id)
             FROM collections c ORDER BY c.created_at, c.id",
        )?;
        let rows = stmt.query_map([], |r| Ok(Collection { id: r.get(0)?, name: r.get(1)?, item_count: r.get(2)? }))?;
        Ok(rows.collect::<std::result::Result<_, _>>()?)
    }

    pub fn create_collection(&self, name: &str) -> Result<Collection> {
        let name = name.trim();
        if name.is_empty() {
            return Err(VaultError::EmptyName);
        }
        let conn = self.conn();
        let exists: Option<i64> = conn
            .query_row("SELECT id FROM collections WHERE name = ?1", [name], |r| r.get(0))
            .optional()?;
        if exists.is_some() {
            return Err(VaultError::DuplicateCollection(name.to_owned()));
        }
        conn.execute("INSERT INTO collections (name, created_at) VALUES (?1, ?2)", params![name, now()])?;
        Ok(Collection { id: conn.last_insert_rowid(), name: name.to_owned(), item_count: 0 })
    }

    /// Find a Collection by name, creating it if needed.
    pub fn collection_named(&self, name: &str) -> Result<Collection> {
        let found = self.collections()?.into_iter().find(|c| c.name.eq_ignore_ascii_case(name.trim()));
        match found {
            Some(c) => Ok(c),
            None => self.create_collection(name),
        }
    }

    // ---------- Vault items ----------

    /// Number of saved links (what "All" shows).
    pub fn item_count(&self) -> Result<i64> {
        Ok(self.conn().query_row("SELECT count(*) FROM items", [], |r| r.get(0))?)
    }

    /// Save a link once. `collection_id` None = in no Collection (still shown in All).
    pub fn add_item(&self, collection_id: Option<i64>, title: &str, url: &str, tags: &[String]) -> Result<VaultItem> {
        let mut conn = self.conn();
        let tx = conn.transaction()?;
        let created = now();
        tx.execute(
            "INSERT INTO items (collection_id, title, url, created_at) VALUES (?1, ?2, ?3, ?4)",
            params![collection_id, title, url, created],
        )?;
        let id = tx.last_insert_rowid();
        let mut clean: Vec<String> = Vec::new();
        for t in tags {
            let t = t.trim().to_lowercase();
            if !t.is_empty() && !clean.contains(&t) {
                tx.execute("INSERT INTO item_tags (item_id, tag) VALUES (?1, ?2)", params![id, t])?;
                clean.push(t);
            }
        }
        tx.commit()?;
        Ok(VaultItem { id, collection_id, title: title.to_owned(), url: url.to_owned(), tags: clean, created_at: created })
    }

    pub fn contains_url(&self, url: &str) -> Result<bool> {
        Ok(self.conn().query_row("SELECT EXISTS(SELECT 1 FROM items WHERE url = ?1)", [url], |r| r.get(0))?)
    }

    /// Items in one Collection, or every saved link when `collection_id` is None ("All").
    pub fn items(&self, collection_id: Option<i64>) -> Result<Vec<VaultItem>> {
        match collection_id {
            Some(id) => self.query_items("WHERE i.collection_id = ?1", params![id]),
            None => self.query_items("", params![]),
        }
    }

    pub fn recent_items(&self, limit: i64) -> Result<Vec<VaultItem>> {
        let sql = format!(" LIMIT {}", limit.max(0));
        self.query_items(&sql, params![])
    }

    /// Case-insensitive search across titles, URLs and tags.
    pub fn search(&self, query: &str) -> Result<Vec<VaultItem>> {
        let like = like_pattern(query);
        self.query_items(
            "WHERE i.title LIKE ?1 ESCAPE '\\' OR i.url LIKE ?1 ESCAPE '\\'
               OR EXISTS (SELECT 1 FROM item_tags t WHERE t.item_id = i.id AND t.tag LIKE ?1 ESCAPE '\\')",
            params![like],
        )
    }

    pub fn delete_item(&self, id: i64) -> Result<()> {
        self.conn().execute("DELETE FROM items WHERE id = ?1", [id])?;
        Ok(())
    }

    fn query_items(&self, filter: &str, args: &[&dyn rusqlite::ToSql]) -> Result<Vec<VaultItem>> {
        let conn = self.conn();
        let (where_part, limit_part) = match filter.find(" LIMIT ") {
            Some(i) => (&filter[..i], &filter[i..]),
            None => (filter, ""),
        };
        let sql = format!(
            "SELECT i.id, i.collection_id, i.title, i.url, i.created_at,
                    COALESCE((SELECT group_concat(tag, char(31)) FROM item_tags t WHERE t.item_id = i.id), '')
             FROM items i {where_part} ORDER BY i.created_at DESC, i.id DESC{limit_part}"
        );
        let mut stmt = conn.prepare(&sql)?;
        let rows = stmt.query_map(args, |r| {
            let tags: String = r.get(5)?;
            Ok(VaultItem {
                id: r.get(0)?,
                collection_id: r.get(1)?,
                title: r.get(2)?,
                url: r.get(3)?,
                created_at: r.get(4)?,
                tags: if tags.is_empty() { vec![] } else { tags.split('\u{1f}').map(str::to_owned).collect() },
            })
        })?;
        Ok(rows.collect::<std::result::Result<_, _>>()?)
    }

    // ---------- History ----------

    /// Record a visit. One entry per URL (title kept fresh) plus one visit row per visit.
    pub fn record_visit(&self, url: &str, title: &str, host: &str, at: i64) -> Result<()> {
        let mut conn = self.conn();
        let tx = conn.transaction()?;
        tx.execute(
            "INSERT INTO history_entries (url, host, title, last_visit) VALUES (?1, ?2, ?3, ?4)
             ON CONFLICT(url) DO UPDATE SET last_visit = excluded.last_visit,
               title = CASE WHEN excluded.title <> '' THEN excluded.title ELSE history_entries.title END",
            params![url, host, title, at],
        )?;
        let entry_id: i64 = tx.query_row("SELECT id FROM history_entries WHERE url = ?1", [url], |r| r.get(0))?;
        tx.execute("INSERT INTO history_visits (entry_id, visited_at) VALUES (?1, ?2)", params![entry_id, at])?;
        tx.commit()?;
        Ok(())
    }

    /// Update a page's title once it's known (titles often arrive after the visit is recorded).
    pub fn update_title(&self, url: &str, title: &str) -> Result<()> {
        if !title.trim().is_empty() {
            self.conn().execute("UPDATE history_entries SET title = ?2 WHERE url = ?1", params![url, title])?;
        }
        Ok(())
    }

    /// Visits newest first, optionally filtered by text in the title, URL or host.
    pub fn history(&self, query: Option<&str>, limit: i64) -> Result<Vec<HistoryVisit>> {
        let conn = self.conn();
        let base = "SELECT v.id, e.url, e.title, e.host, v.visited_at
                    FROM history_visits v JOIN history_entries e ON e.id = v.entry_id";
        let map = |r: &rusqlite::Row| -> rusqlite::Result<HistoryVisit> {
            Ok(HistoryVisit { visit_id: r.get(0)?, url: r.get(1)?, title: r.get(2)?, host: r.get(3)?, visited_at: r.get(4)? })
        };
        let rows: Vec<HistoryVisit> = match query.map(str::trim).filter(|q| !q.is_empty()) {
            Some(q) => {
                let sql = format!(
                    "{base} WHERE e.title LIKE ?1 ESCAPE '\\' OR e.url LIKE ?1 ESCAPE '\\'
                     ORDER BY v.visited_at DESC, v.id DESC LIMIT ?2"
                );
                let mut stmt = conn.prepare(&sql)?;
                let r = stmt.query_map(params![like_pattern(q), limit], map)?.collect::<std::result::Result<_, _>>()?;
                r
            }
            None => {
                let sql = format!("{base} ORDER BY v.visited_at DESC, v.id DESC LIMIT ?1");
                let mut stmt = conn.prepare(&sql)?;
                let r = stmt.query_map(params![limit], map)?.collect::<std::result::Result<_, _>>()?;
                r
            }
        };
        Ok(rows)
    }

    /// Delete one visit; the page entry goes too once it has no visits left.
    pub fn delete_visit(&self, visit_id: i64) -> Result<()> {
        let conn = self.conn();
        conn.execute("DELETE FROM history_visits WHERE id = ?1", [visit_id])?;
        drop_orphans(&conn)?;
        checkpoint(&conn)
    }

    /// Delete every visit to a site and its subdomains.
    pub fn delete_history_for_host(&self, host: &str) -> Result<()> {
        let conn = self.conn();
        conn.execute(&format!("DELETE FROM history_entries WHERE {HOST_MATCH}"), [host])?;
        checkpoint(&conn)
    }

    /// Delete all history, then empty the write-ahead log so no copy of it stays in the -wal file.
    pub fn clear_history(&self) -> Result<()> {
        let conn = self.conn();
        conn.execute_batch("DELETE FROM history_visits; DELETE FROM history_entries;")?;
        checkpoint(&conn)
    }

    // ---------- Passwords (logins) ----------

    /// Save a new login. The blobs are already encrypted by the app.
    pub fn add_login(&self, origin: &str, username_enc: &[u8], password_enc: &[u8]) -> Result<Login> {
        let conn = self.conn();
        let at = now();
        conn.execute(
            "INSERT INTO logins (origin, username_enc, password_enc, created_at, last_used_at, times_used)
             VALUES (?1, ?2, ?3, ?4, ?4, 0)",
            params![origin, username_enc, password_enc, at],
        )?;
        Ok(Login {
            id: conn.last_insert_rowid(),
            origin: origin.to_owned(),
            username_enc: username_enc.to_vec(),
            password_enc: password_enc.to_vec(),
            created_at: at,
            last_used_at: at,
            times_used: 0,
        })
    }

    /// Replace a login's username and password (the site stays the same).
    pub fn update_login(&self, id: i64, username_enc: &[u8], password_enc: &[u8]) -> Result<()> {
        let conn = self.conn();
        conn.execute(
            "UPDATE logins SET username_enc = ?2, password_enc = ?3 WHERE id = ?1",
            params![id, username_enc, password_enc],
        )?;
        // The old encrypted values are overwritten (secure_delete); empty the log too.
        checkpoint(&conn)
    }

    pub fn delete_login(&self, id: i64) -> Result<()> {
        let conn = self.conn();
        conn.execute("DELETE FROM logins WHERE id = ?1", [id])?;
        checkpoint(&conn)
    }

    /// Delete every saved login ("Reset passwords" when they can no longer be decrypted).
    pub fn delete_all_logins(&self) -> Result<()> {
        let conn = self.conn();
        conn.execute("DELETE FROM logins", [])?;
        checkpoint(&conn)
    }

    /// Logins for exactly this origin, most recently used first.
    pub fn logins_for_origin(&self, origin: &str) -> Result<Vec<Login>> {
        let conn = self.conn();
        let mut stmt = conn.prepare(&format!("SELECT {LOGIN_COLUMNS} FROM logins WHERE origin = ?1 {LOGIN_ORDER}"))?;
        let rows = stmt.query_map([origin], login_row)?;
        Ok(rows.collect::<std::result::Result<_, _>>()?)
    }

    /// Every login, most recently used first, then most used.
    pub fn all_logins(&self) -> Result<Vec<Login>> {
        let conn = self.conn();
        let mut stmt = conn.prepare(&format!("SELECT {LOGIN_COLUMNS} FROM logins {LOGIN_ORDER}"))?;
        let rows = stmt.query_map([], login_row)?;
        Ok(rows.collect::<std::result::Result<_, _>>()?)
    }

    pub fn login_count(&self) -> Result<i64> {
        Ok(self.conn().query_row("SELECT count(*) FROM logins", [], |r| r.get(0))?)
    }

    /// A login was used (filled): note when, and count it.
    pub fn touch_login(&self, id: i64) -> Result<()> {
        self.conn().execute(
            "UPDATE logins SET last_used_at = ?2, times_used = times_used + 1 WHERE id = ?1",
            params![id, now()],
        )?;
        Ok(())
    }

    // ---------- key-value area ----------

    pub fn kv_get(&self, key: &str) -> Result<Option<String>> {
        Ok(self.conn().query_row("SELECT value FROM kv WHERE key = ?1", [key], |r| r.get(0)).optional()?)
    }

    pub fn kv_set(&self, key: &str, value: &str) -> Result<()> {
        self.conn().execute(
            "INSERT INTO kv (key, value) VALUES (?1, ?2) ON CONFLICT(key) DO UPDATE SET value = excluded.value",
            params![key, value],
        )?;
        Ok(())
    }

    pub fn kv_delete(&self, key: &str) -> Result<()> {
        self.conn().execute("DELETE FROM kv WHERE key = ?1", [key])?;
        Ok(())
    }
}

fn like_pattern(query: &str) -> String {
    format!("%{}%", query.trim().replace('\\', "\\\\").replace('%', "\\%").replace('_', "\\_"))
}

/// Copy the write-ahead log into the database and empty it, so deleted rows don't survive in the
/// -wal file (with `secure_delete` on, they are zeroed in the database itself).
fn checkpoint(conn: &Connection) -> Result<()> {
    conn.query_row("PRAGMA wal_checkpoint(TRUNCATE)", [], |_| Ok(()))?;
    Ok(())
}

fn drop_orphans(conn: &Connection) -> Result<()> {
    conn.execute(
        "DELETE FROM history_entries WHERE NOT EXISTS (SELECT 1 FROM history_visits v WHERE v.entry_id = history_entries.id)",
        [],
    )?;
    Ok(())
}

/// Bring any older database up to [SCHEMA_VERSION] without losing data.
fn migrate(conn: &Connection) -> Result<()> {
    let version: i64 = conn.query_row("PRAGMA user_version", [], |r| r.get(0))?;
    if version >= SCHEMA_VERSION {
        conn.execute_batch(SCHEMA)?;
        return Ok(());
    }
    // Only a version-0 database (user_version was never set) needs the items rebuild below.
    let has_items: bool = version < 2 && conn.query_row(
        "SELECT EXISTS(SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = 'items')",
        [],
        |r| r.get::<_, bool>(0),
    )?;
    if has_items {
        // Version 1 (before "All"): every item had to be in a Collection, with an "Unsorted"
        // catch-all. Rebuild `items` with an optional Collection, move Unsorted items to
        // "no Collection" (they stay visible in All), then drop the Unsorted Collection.
        conn.execute_batch(
            "PRAGMA foreign_keys = OFF;
             BEGIN;
             CREATE TABLE items_v2 (
                 id            INTEGER PRIMARY KEY,
                 collection_id INTEGER REFERENCES collections(id) ON DELETE SET NULL,
                 title         TEXT NOT NULL,
                 url           TEXT NOT NULL,
                 created_at    INTEGER NOT NULL
             );
             INSERT INTO items_v2 (id, collection_id, title, url, created_at)
                 SELECT i.id,
                        CASE WHEN c.name = 'Unsorted' THEN NULL ELSE i.collection_id END,
                        i.title, i.url, i.created_at
                 FROM items i LEFT JOIN collections c ON c.id = i.collection_id;
             DROP TABLE items;
             ALTER TABLE items_v2 RENAME TO items;
             DELETE FROM collections WHERE name = 'Unsorted';
             COMMIT;
             PRAGMA foreign_keys = ON;",
        )?;
    }
    // Version 2 → 3 only adds the `logins` table: CREATE TABLE IF NOT EXISTS in SCHEMA does it,
    // leaving everything already stored untouched.
    conn.execute_batch(SCHEMA)?;
    conn.execute_batch(&format!("PRAGMA user_version = {SCHEMA_VERSION};"))?;
    Ok(())
}

#[cfg(test)]
mod tests {
    use super::*;

    fn temp_path(name: &str) -> String {
        let dir = std::env::temp_dir().join(format!("flush-store-test-{}-{}", std::process::id(), name));
        let _ = std::fs::remove_file(&dir);
        dir.to_string_lossy().into_owned()
    }

    #[test]
    fn new_store_has_no_collections_and_all_is_every_link() {
        let s = Store::open(&temp_path("all")).unwrap();
        assert!(s.collections().unwrap().is_empty());
        let later = s.create_collection("Watch later").unwrap();
        s.add_item(Some(later.id), "In a collection", "https://a.example/1", &[]).unwrap();
        s.add_item(None, "In no collection", "https://a.example/2", &[]).unwrap();
        // Each link stored once; All shows both; the Collection shows only its own.
        assert_eq!(s.item_count().unwrap(), 2);
        assert_eq!(s.items(None).unwrap().len(), 2);
        assert_eq!(s.items(Some(later.id)).unwrap().len(), 1);
    }

    #[test]
    fn save_find_and_count() {
        let s = Store::open(&temp_path("save")).unwrap();
        let later = s.create_collection("Watch later").unwrap();
        s.add_item(Some(later.id), "Rainy window ambience", "https://example.com/v/3", &["Chill".into(), "sleep".into(), "chill".into()]).unwrap();
        s.add_item(None, "Ten snacks", "https://example.com/a/3", &[]).unwrap();
        let items = s.items(Some(later.id)).unwrap();
        assert_eq!(items[0].tags, vec!["chill".to_string(), "sleep".to_string()]);
        assert_eq!(s.collections().unwrap()[0].item_count, 1);
        assert!(s.contains_url("https://example.com/a/3").unwrap());
        assert_eq!(s.search("SLEEP").unwrap().len(), 1);
        assert_eq!(s.recent_items(1).unwrap().len(), 1);
    }

    #[test]
    fn deleting_a_collection_keeps_its_links_in_all() {
        let s = Store::open(&temp_path("delcoll")).unwrap();
        let c = s.create_collection("Temp").unwrap();
        s.add_item(Some(c.id), "Kept", "https://example.com/", &[]).unwrap();
        s.conn().execute("DELETE FROM collections WHERE id = ?1", [c.id]).unwrap();
        let all = s.items(None).unwrap();
        assert_eq!(all.len(), 1);
        assert_eq!(all[0].collection_id, None);
    }

    #[test]
    fn duplicate_collection_rejected() {
        let s = Store::open(&temp_path("dup")).unwrap();
        s.create_collection("Inspo").unwrap();
        assert!(matches!(s.create_collection("inspo"), Err(VaultError::DuplicateCollection(_))));
        assert_eq!(s.collection_named("INSPO").unwrap().name, "Inspo");
    }

    #[test]
    fn kv_round_trip() {
        let s = Store::open(&temp_path("kv")).unwrap();
        s.kv_set("keep_list", "[]").unwrap();
        assert_eq!(s.kv_get("keep_list").unwrap().as_deref(), Some("[]"));
        s.kv_delete("keep_list").unwrap();
        assert_eq!(s.kv_get("keep_list").unwrap(), None);
    }

    #[test]
    fn upgrades_v1_database_moving_unsorted_to_all() {
        let path = temp_path("migrate");
        {
            // Build a version-1 database by hand: NOT NULL collection, "Unsorted" catch-all.
            let c = Connection::open(&path).unwrap();
            c.execute_batch(
                "CREATE TABLE collections (id INTEGER PRIMARY KEY, name TEXT NOT NULL UNIQUE COLLATE NOCASE, created_at INTEGER NOT NULL);
                 CREATE TABLE items (id INTEGER PRIMARY KEY, collection_id INTEGER NOT NULL REFERENCES collections(id) ON DELETE CASCADE,
                                     title TEXT NOT NULL, url TEXT NOT NULL, created_at INTEGER NOT NULL);
                 CREATE TABLE item_tags (item_id INTEGER NOT NULL REFERENCES items(id) ON DELETE CASCADE, tag TEXT NOT NULL, PRIMARY KEY (item_id, tag));
                 CREATE TABLE kv (key TEXT PRIMARY KEY, value TEXT NOT NULL);
                 INSERT INTO collections VALUES (1, 'Unsorted', 0), (2, 'Watch later', 1);
                 INSERT INTO items VALUES (10, 1, 'Was unsorted', 'https://a.example/', 5), (11, 2, 'In watch later', 'https://b.example/', 6);
                 INSERT INTO item_tags VALUES (11, 'chill');",
            )
            .unwrap();
        }
        let s = Store::open(&path).unwrap();
        let names: Vec<String> = s.collections().unwrap().into_iter().map(|c| c.name).collect();
        assert_eq!(names, vec!["Watch later".to_string()]); // Unsorted is gone
        let all = s.items(None).unwrap();
        assert_eq!(all.len(), 2); // nothing lost
        let was_unsorted = all.iter().find(|i| i.id == 10).unwrap();
        assert_eq!(was_unsorted.collection_id, None);
        let kept = all.iter().find(|i| i.id == 11).unwrap();
        assert_eq!(kept.collection_id, Some(2));
        assert_eq!(kept.tags, vec!["chill".to_string()]);
        // Opening again is a no-op.
        drop(s);
        assert_eq!(Store::open(&path).unwrap().items(None).unwrap().len(), 2);
    }

    #[test]
    fn history_records_searches_and_deletes_completely() {
        let s = Store::open(&temp_path("history")).unwrap();
        s.record_visit("https://news.example.com/a", "", "news.example.com", 1_000).unwrap();
        s.update_title("https://news.example.com/a", "Story A").unwrap();
        s.record_visit("https://news.example.com/a", "", "news.example.com", 2_000).unwrap(); // same page again
        s.record_visit("https://shop.example.org/", "Shop", "shop.example.org", 3_000).unwrap();
        s.record_visit("https://m.example.com/x", "Mobile", "m.example.com", 4_000).unwrap();

        let all = s.history(None, 100).unwrap();
        assert_eq!(all.len(), 4); // every visit listed, newest first
        assert_eq!(all[0].visited_at, 4_000);
        assert_eq!(all.iter().filter(|v| v.title == "Story A").count(), 2); // title kept after empty update
        assert_eq!(s.history(Some("shop"), 100).unwrap().len(), 1);

        // Deleting one visit leaves the page while other visits remain.
        let shop = all.iter().find(|v| v.host == "shop.example.org").unwrap().visit_id;
        s.delete_visit(shop).unwrap();
        let pages: i64 = s.conn().query_row("SELECT count(*) FROM history_entries", [], |r| r.get(0)).unwrap();
        assert_eq!(pages, 2); // shop.example.org page removed with its only visit, no orphan left

        // Site deletion covers subdomains.
        s.delete_history_for_host("example.com").unwrap();
        assert!(s.history(None, 100).unwrap().is_empty());
        let orphans: i64 = s.conn().query_row("SELECT count(*) FROM history_visits", [], |r| r.get(0)).unwrap();
        assert_eq!(orphans, 0);
    }

    #[test]
    fn cleared_history_leaves_no_trace_in_the_files() {
        let path = temp_path("trace");
        let s = Store::open(&path).unwrap();
        s.record_visit("https://secret-marker.example/page", "SECRETTITLEMARKER", "secret-marker.example", 1).unwrap();
        s.record_visit("https://one-visit.example/", "ONEVISITMARKER", "one-visit.example", 2).unwrap();
        s.record_visit("https://whole-site.example/a", "SITEMARKER", "whole-site.example", 3).unwrap();
        s.record_visit("https://keep.example/", "Kept", "keep.example", 4).unwrap();
        let one = s.history(Some("ONEVISITMARKER"), 10).unwrap()[0].visit_id;
        s.delete_visit(one).unwrap();
        s.delete_history_for_host("whole-site.example").unwrap();
        for f in [path.clone(), format!("{path}-wal")] {
            let text = String::from_utf8_lossy(&std::fs::read(&f).unwrap_or_default()).into_owned();
            assert!(!text.contains("ONEVISITMARKER") && !text.contains("SITEMARKER"), "single delete left a trace in {f}");
        }
        s.clear_history().unwrap();
        for f in [path.clone(), format!("{path}-wal")] {
            let bytes = std::fs::read(&f).unwrap_or_default();
            let text = String::from_utf8_lossy(&bytes);
            assert!(!text.contains("SECRETTITLEMARKER") && !text.contains("secret-marker"), "trace left in {f}");
        }
    }

    #[test]
    fn clear_history_removes_everything() {
        let s = Store::open(&temp_path("clear")).unwrap();
        s.record_visit("https://a.example/", "A", "a.example", 10).unwrap();
        s.record_visit("https://b.example/", "B", "b.example", 20).unwrap();
        s.clear_history().unwrap();
        assert!(s.history(None, 100).unwrap().is_empty());
        let rows: i64 = s.conn().query_row("SELECT (SELECT count(*) FROM history_entries) + (SELECT count(*) FROM history_visits)", [], |r| r.get(0)).unwrap();
        assert_eq!(rows, 0);
    }

    #[test]
    fn logins_add_list_update_touch_delete() {
        let s = Store::open(&temp_path("logins")).unwrap();
        let a = s.add_login("https://example.com", b"u1-enc", b"p1-enc").unwrap();
        let b = s.add_login("https://example.com", b"u2-enc", b"p2-enc").unwrap();
        s.add_login("https://example.com:8443", b"u3-enc", b"p3-enc").unwrap();
        s.add_login("https://other.example", b"u4-enc", b"p4-enc").unwrap();
        assert_eq!(s.login_count().unwrap(), 4);
        // Exact origin only: another port or site is a different origin.
        let here = s.logins_for_origin("https://example.com").unwrap();
        assert_eq!(here.len(), 2);
        assert!(s.logins_for_origin("https://www.example.com").unwrap().is_empty());
        assert_eq!(here.iter().find(|l| l.id == a.id).unwrap().username_enc, b"u1-enc".to_vec());

        // Using a login moves it to the top and counts it.
        s.touch_login(a.id).unwrap();
        s.touch_login(a.id).unwrap();
        let all = s.all_logins().unwrap();
        assert_eq!(all[0].id, a.id);
        assert_eq!(all[0].times_used, 2);

        s.update_login(b.id, b"u2-enc", b"p2-new").unwrap();
        let b2 = s.logins_for_origin("https://example.com").unwrap().into_iter().find(|l| l.id == b.id).unwrap();
        assert_eq!(b2.password_enc, b"p2-new".to_vec());

        s.delete_login(b.id).unwrap();
        assert_eq!(s.login_count().unwrap(), 3);
        s.delete_all_logins().unwrap();
        assert_eq!(s.login_count().unwrap(), 0);
    }

    #[test]
    fn deleted_login_leaves_no_trace_in_the_files() {
        let path = temp_path("login-trace");
        let s = Store::open(&path).unwrap();
        let l = s.add_login("https://trace.example", b"USERBLOBMARKER", b"PASSBLOBMARKER").unwrap();
        s.update_login(l.id, b"USERBLOBMARKER", b"NEWPASSMARKER").unwrap();
        for f in [path.clone(), format!("{path}-wal")] {
            let text = String::from_utf8_lossy(&std::fs::read(&f).unwrap_or_default()).into_owned();
            assert!(!text.contains("PASSBLOBMARKER"), "old password blob left in {f}");
        }
        s.delete_login(l.id).unwrap();
        for f in [path.clone(), format!("{path}-wal")] {
            let text = String::from_utf8_lossy(&std::fs::read(&f).unwrap_or_default()).into_owned();
            assert!(!text.contains("NEWPASSMARKER") && !text.contains("USERBLOBMARKER"), "login left in {f}");
        }
    }

    #[test]
    fn upgrades_v2_database_adding_logins_and_keeping_everything() {
        let path = temp_path("migrate-v2");
        {
            // A version-2 database as the previous release wrote it (no logins table).
            let c = Connection::open(&path).unwrap();
            c.execute_batch(
                "CREATE TABLE collections (id INTEGER PRIMARY KEY, name TEXT NOT NULL UNIQUE COLLATE NOCASE, created_at INTEGER NOT NULL);
                 CREATE TABLE items (id INTEGER PRIMARY KEY, collection_id INTEGER REFERENCES collections(id) ON DELETE SET NULL,
                                     title TEXT NOT NULL, url TEXT NOT NULL, created_at INTEGER NOT NULL);
                 CREATE TABLE item_tags (item_id INTEGER NOT NULL REFERENCES items(id) ON DELETE CASCADE, tag TEXT NOT NULL, PRIMARY KEY (item_id, tag));
                 CREATE TABLE kv (key TEXT PRIMARY KEY, value TEXT NOT NULL);
                 CREATE TABLE history_entries (id INTEGER PRIMARY KEY, url TEXT NOT NULL UNIQUE, host TEXT NOT NULL, title TEXT NOT NULL, last_visit INTEGER NOT NULL);
                 CREATE TABLE history_visits (id INTEGER PRIMARY KEY, entry_id INTEGER NOT NULL REFERENCES history_entries(id) ON DELETE CASCADE, visited_at INTEGER NOT NULL);
                 -- A user Collection that happens to be called Unsorted must survive (the v0 rebuild must not run again).
                 INSERT INTO collections VALUES (1, 'Unsorted', 0), (2, 'Watch later', 1);
                 INSERT INTO items VALUES (10, 1, 'In my Unsorted', 'https://a.example/', 5), (11, NULL, 'In no collection', 'https://b.example/', 6);
                 INSERT INTO item_tags VALUES (11, 'chill');
                 INSERT INTO kv VALUES ('keep_list', '[\"mail.example.com\"]');
                 INSERT INTO history_entries VALUES (1, 'https://h.example/', 'h.example', 'H', 7);
                 INSERT INTO history_visits VALUES (1, 1, 7);
                 PRAGMA user_version = 2;",
            )
            .unwrap();
        }
        let s = Store::open(&path).unwrap();
        let version: i64 = s.conn().query_row("PRAGMA user_version", [], |r| r.get(0)).unwrap();
        assert_eq!(version, 3);
        let names: Vec<String> = s.collections().unwrap().into_iter().map(|c| c.name).collect();
        assert_eq!(names, vec!["Unsorted".to_string(), "Watch later".to_string()]);
        let all = s.items(None).unwrap();
        assert_eq!(all.len(), 2);
        assert_eq!(all.iter().find(|i| i.id == 10).unwrap().collection_id, Some(1));
        assert_eq!(all.iter().find(|i| i.id == 11).unwrap().tags, vec!["chill".to_string()]);
        assert_eq!(s.kv_get("keep_list").unwrap().as_deref(), Some("[\"mail.example.com\"]"));
        assert_eq!(s.history(None, 10).unwrap().len(), 1);
        // The new table works.
        assert_eq!(s.login_count().unwrap(), 0);
        s.add_login("https://example.com", b"u", b"p").unwrap();
        drop(s);
        let s = Store::open(&path).unwrap();
        assert_eq!(s.login_count().unwrap(), 1);
        assert_eq!(s.items(None).unwrap().len(), 2);
    }
}
