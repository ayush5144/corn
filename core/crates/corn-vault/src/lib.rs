// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

//! corn-vault: Corn's local database (SQLite).
//!
//! Holds everything Corn keeps that could reveal browsing:
//! - the **Vault**: saved links (Vault Items), each stored once, optionally in one Collection.
//!   "All" is not stored: it is simply every saved link.
//! - **History**: one entry per page plus a visit log with timestamps (cleared by Flush).
//!   Deletion is all-or-nothing, per visit, or per site (history only): no time ranges.
//! - **Favourites**: a short, ordered list of links shown as tiles on the new tab. Its own list,
//!   not Vault Items: a favourite doesn't have to be saved in the Vault. Survives every Flush and,
//!   unlike the Vault, is shown without the PIN. Each favourite's site may have one small icon
//!   (`favorite_icons`, one per site, only while a favourite on that site exists).
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

pub mod export;

/// Schema version written to `PRAGMA user_version`.
///
/// - 0: first release (every Vault Item in a Collection, "Unsorted" catch-all).
/// - 2: optional Collection ("All"), history.
/// - 3: `logins` (saved passwords).
/// - 4: `logins.password_changed_at` (for exports and "password last changed").
/// - 5: `favorites` (the new tab's Favourites, their own list) and `favorite_icons` (one per site).
const SCHEMA_VERSION: i64 = 5;

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

/// One Favourite: a link shown as a tile on the new tab. `pos`: lower comes first.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Favorite {
    pub id: i64,
    pub url: String,
    pub title: String,
    pub pos: i64,
    pub created_at: i64,
}

/// A favourite site's icon: a 64×64 PNG taken from a page Corn showed (never downloaded on its own).
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct FavoriteIcon {
    /// Lower-case host without "www." (see [site_host]).
    pub host: String,
    pub png: Vec<u8>,
    pub updated_at: i64,
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
    /// When the password was last saved or changed (milliseconds). Not touched when only the username changes.
    pub password_changed_at: i64,
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
    times_used   INTEGER NOT NULL DEFAULT 0,
    password_changed_at INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS logins_by_origin ON logins(origin);
CREATE TABLE IF NOT EXISTS favorites (
    id         INTEGER PRIMARY KEY,
    url        TEXT NOT NULL UNIQUE,
    title      TEXT NOT NULL,
    pos        INTEGER NOT NULL,
    created_at INTEGER NOT NULL
);
CREATE TABLE IF NOT EXISTS favorite_icons (
    host       TEXT PRIMARY KEY,
    png        BLOB NOT NULL,
    updated_at INTEGER NOT NULL
);
";

const LOGIN_COLUMNS: &str = "id, origin, username_enc, password_enc, created_at, last_used_at, times_used, password_changed_at";
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
        password_changed_at: r.get(7)?,
    })
}

/// The site a favourite or page belongs to, as icons are keyed: lower-case host without "www."
/// (`https://WWW.Example.com:8080/a` → `example.com`). A bare host is returned normalised too.
pub fn site_host(url: &str) -> String {
    let rest = url.trim().split_once("://").map_or(url.trim(), |(_, r)| r);
    let authority = rest.split(['/', '?', '#']).next().unwrap_or("");
    let host_port = authority.rsplit_once('@').map_or(authority, |(_, h)| h);
    let host = if host_port.starts_with('[') {
        host_port.split_inclusive(']').next().unwrap_or(host_port)
    } else {
        host_port.split(':').next().unwrap_or("")
    };
    let host = host.to_ascii_lowercase();
    host.strip_prefix("www.").map(str::to_owned).unwrap_or(host)
}

/// The sites that have at least one favourite.
fn favorite_hosts(conn: &Connection) -> Result<std::collections::HashSet<String>> {
    let mut stmt = conn.prepare("SELECT url FROM favorites")?;
    let rows = stmt.query_map([], |r| r.get::<_, String>(0))?;
    let mut out = std::collections::HashSet::new();
    for url in rows {
        out.insert(site_host(&url?));
    }
    Ok(out)
}

fn favorite_row(r: &rusqlite::Row) -> rusqlite::Result<Favorite> {
    Ok(Favorite { id: r.get(0)?, url: r.get(1)?, title: r.get(2)?, pos: r.get(3)?, created_at: r.get(4)? })
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

    // ---------- Favourites ----------

    /// Add a link at the end of the Favourites. Adding one that is already there keeps its place
    /// and takes the new title (if not blank). Returns the favourite.
    pub fn add_favorite(&self, url: &str, title: &str) -> Result<Favorite> {
        let conn = self.conn();
        conn.execute(
            "INSERT INTO favorites (url, title, pos, created_at)
             VALUES (?1, ?2, (SELECT COALESCE(MAX(pos), 0) + 1 FROM favorites), ?3)
             ON CONFLICT(url) DO UPDATE SET
               title = CASE WHEN trim(excluded.title) <> '' THEN excluded.title ELSE favorites.title END",
            params![url, title, now()],
        )?;
        Ok(conn.query_row(
            "SELECT id, url, title, pos, created_at FROM favorites WHERE url = ?1",
            [url],
            favorite_row,
        )?)
    }

    /// Remove a favourite; its site's icon goes too once no favourite on that site is left.
    pub fn remove_favorite(&self, id: i64) -> Result<()> {
        let conn = self.conn();
        conn.execute("DELETE FROM favorites WHERE id = ?1", [id])?;
        let in_use = favorite_hosts(&conn)?;
        let icons: Vec<String> = {
            let mut stmt = conn.prepare("SELECT host FROM favorite_icons")?;
            let rows = stmt.query_map([], |r| r.get(0))?;
            rows.collect::<std::result::Result<_, _>>()?
        };
        for host in icons.iter().filter(|h| !in_use.contains(*h)) {
            conn.execute("DELETE FROM favorite_icons WHERE host = ?1", [host])?;
        }
        Ok(())
    }

    /// Save (or replace) a site's icon, only if a favourite on that site exists: icons are never
    /// kept for anything else. `host` is normalised with [site_host]. Returns whether it was saved.
    pub fn set_favorite_icon(&self, host: &str, png: &[u8]) -> Result<bool> {
        let host = site_host(host);
        let conn = self.conn();
        if host.is_empty() || !favorite_hosts(&conn)?.contains(&host) {
            return Ok(false);
        }
        conn.execute(
            "INSERT INTO favorite_icons (host, png, updated_at) VALUES (?1, ?2, ?3)
             ON CONFLICT(host) DO UPDATE SET png = excluded.png, updated_at = excluded.updated_at",
            params![host, png, now()],
        )?;
        Ok(true)
    }

    /// True when a favourite on this site has no icon yet, or one older than `max_age_ms`.
    pub fn favorite_icon_wanted(&self, host: &str, max_age_ms: i64) -> Result<bool> {
        let host = site_host(host);
        let conn = self.conn();
        if host.is_empty() || !favorite_hosts(&conn)?.contains(&host) {
            return Ok(false);
        }
        let updated: Option<i64> =
            conn.query_row("SELECT updated_at FROM favorite_icons WHERE host = ?1", [&host], |r| r.get(0)).optional()?;
        Ok(updated.map_or(true, |at| now() - at > max_age_ms))
    }

    /// Every stored icon (one per favourite site).
    pub fn favorite_icons(&self) -> Result<Vec<FavoriteIcon>> {
        let conn = self.conn();
        let mut stmt = conn.prepare("SELECT host, png, updated_at FROM favorite_icons")?;
        let rows = stmt.query_map([], |r| Ok(FavoriteIcon { host: r.get(0)?, png: r.get(1)?, updated_at: r.get(2)? }))?;
        Ok(rows.collect::<std::result::Result<_, _>>()?)
    }

    /// Every Favourite in new-tab order (first = leftmost).
    pub fn favorites(&self) -> Result<Vec<Favorite>> {
        let conn = self.conn();
        let mut stmt = conn.prepare("SELECT id, url, title, pos, created_at FROM favorites ORDER BY pos, id")?;
        let rows = stmt.query_map([], favorite_row)?;
        Ok(rows.collect::<std::result::Result<_, _>>()?)
    }

    /// Move a Favourite `delta` places (negative = left/earlier), shifting the ones in between;
    /// for ±1 that is a swap with the neighbour. Stops at either end; an unknown id does nothing.
    pub fn move_favorite(&self, id: i64, delta: i64) -> Result<()> {
        let mut conn = self.conn();
        let tx = conn.transaction()?;
        let mut ids: Vec<i64> = {
            let mut stmt = tx.prepare("SELECT id FROM favorites ORDER BY pos, id")?;
            let rows = stmt.query_map([], |r| r.get(0))?;
            rows.collect::<std::result::Result<_, _>>()?
        };
        let Some(from) = ids.iter().position(|i| *i == id) else { return Ok(()) };
        let to = (from as i64).saturating_add(delta).clamp(0, ids.len() as i64 - 1) as usize;
        if to == from {
            return Ok(());
        }
        let moved = ids.remove(from);
        ids.insert(to, moved);
        // Renumber 1..n so positions stay unique and small.
        for (pos, fav) in ids.iter().enumerate() {
            tx.execute("UPDATE favorites SET pos = ?2 WHERE id = ?1", params![fav, pos as i64 + 1])?;
        }
        tx.commit()?;
        Ok(())
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
            "INSERT INTO logins (origin, username_enc, password_enc, created_at, last_used_at, times_used, password_changed_at)
             VALUES (?1, ?2, ?3, ?4, ?4, 0, ?4)",
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
            password_changed_at: at,
        })
    }

    /// Replace a login's username and password (the site stays the same). `password_changed`: the
    /// password text itself changed (the app knows; the blobs always differ), so note when.
    pub fn update_login(&self, id: i64, username_enc: &[u8], password_enc: &[u8], password_changed: bool) -> Result<()> {
        let conn = self.conn();
        conn.execute(
            "UPDATE logins SET username_enc = ?2, password_enc = ?3,
                password_changed_at = CASE WHEN ?4 THEN ?5 ELSE password_changed_at END
             WHERE id = ?1",
            params![id, username_enc, password_enc, password_changed, now()],
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

    // ---------- Export (see export.rs) ----------

    /// Every Collection with when it was made, oldest first, for the export.
    fn export_folders(&self) -> Result<Vec<export::Folder>> {
        let conn = self.conn();
        let mut stmt = conn.prepare("SELECT id, name, created_at FROM collections ORDER BY created_at, id")?;
        let rows = stmt.query_map([], |r| Ok(export::Folder { id: r.get(0)?, name: r.get(1)?, created_at: r.get(2)? }))?;
        Ok(rows.collect::<std::result::Result<_, _>>()?)
    }

    /// Every saved link as an HTML bookmarks file (one "Corn Vault" folder, a folder per Collection).
    pub fn export_links_html(&self) -> Result<String> {
        let folders = self.export_folders()?;
        let items = self.items(None)?;
        Ok(export::links_html(&folders, &items, now()))
    }

    /// Every saved link as CSV (`title,url,collection,tags,saved_at`).
    pub fn export_links_csv(&self) -> Result<String> {
        let folders = self.export_folders()?;
        let items = self.items(None)?;
        Ok(export::links_csv(&folders, &items))
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
    // Version 3 → 4: `logins` gains `password_changed_at`, set to when each login was saved.
    // (Before version 3 there is no `logins` table; SCHEMA below creates it with the column.)
    let has_logins: bool = conn.query_row(
        "SELECT EXISTS(SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = 'logins')",
        [],
        |r| r.get(0),
    )?;
    if has_logins {
        let has_column: bool = conn.query_row(
            "SELECT EXISTS(SELECT 1 FROM pragma_table_info('logins') WHERE name = 'password_changed_at')",
            [],
            |r| r.get(0),
        )?;
        if !has_column {
            conn.execute_batch(
                "BEGIN;
                 ALTER TABLE logins ADD COLUMN password_changed_at INTEGER NOT NULL DEFAULT 0;
                 UPDATE logins SET password_changed_at = created_at;
                 COMMIT;",
            )?;
        }
    }
    // Version 2 → 3 only adds the `logins` table, and 4 → 5 only the `favorites` table:
    // CREATE TABLE IF NOT EXISTS in SCHEMA does it, leaving everything already stored untouched.
    conn.execute_batch(SCHEMA)?;
    conn.execute_batch(&format!("PRAGMA user_version = {SCHEMA_VERSION};"))?;
    Ok(())
}

#[cfg(test)]
mod tests {
    use super::*;

    fn temp_path(name: &str) -> String {
        let dir = std::env::temp_dir().join(format!("corn-vault-test-{}-{}", std::process::id(), name));
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

        s.update_login(b.id, b"u2-enc", b"p2-new", true).unwrap();
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
        s.update_login(l.id, b"USERBLOBMARKER", b"NEWPASSMARKER", true).unwrap();
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
    fn password_changed_at_moves_only_when_the_password_changes() {
        let s = Store::open(&temp_path("pw-changed")).unwrap();
        let l = s.add_login("https://example.com", b"u", b"p").unwrap();
        assert_eq!(l.password_changed_at, l.created_at);
        s.conn().execute("UPDATE logins SET password_changed_at = 5 WHERE id = ?1", [l.id]).unwrap();
        s.update_login(l.id, b"u-new", b"p-reencrypted", false).unwrap();
        assert_eq!(s.all_logins().unwrap()[0].password_changed_at, 5); // username only
        s.update_login(l.id, b"u-new", b"p-new", true).unwrap();
        assert!(s.all_logins().unwrap()[0].password_changed_at > 5);
    }

    #[test]
    fn upgrades_v3_database_adding_password_changed_at() {
        let path = temp_path("migrate-v3");
        {
            // A version-3 database exactly as the previous release wrote it.
            let c = Connection::open(&path).unwrap();
            c.execute_batch(
                "CREATE TABLE collections (id INTEGER PRIMARY KEY, name TEXT NOT NULL UNIQUE COLLATE NOCASE, created_at INTEGER NOT NULL);
                 CREATE TABLE items (id INTEGER PRIMARY KEY, collection_id INTEGER REFERENCES collections(id) ON DELETE SET NULL,
                                     title TEXT NOT NULL, url TEXT NOT NULL, created_at INTEGER NOT NULL);
                 CREATE TABLE item_tags (item_id INTEGER NOT NULL REFERENCES items(id) ON DELETE CASCADE, tag TEXT NOT NULL, PRIMARY KEY (item_id, tag));
                 CREATE TABLE kv (key TEXT PRIMARY KEY, value TEXT NOT NULL);
                 CREATE TABLE history_entries (id INTEGER PRIMARY KEY, url TEXT NOT NULL UNIQUE, host TEXT NOT NULL, title TEXT NOT NULL, last_visit INTEGER NOT NULL);
                 CREATE TABLE history_visits (id INTEGER PRIMARY KEY, entry_id INTEGER NOT NULL REFERENCES history_entries(id) ON DELETE CASCADE, visited_at INTEGER NOT NULL);
                 CREATE TABLE logins (id INTEGER PRIMARY KEY, origin TEXT NOT NULL, username_enc BLOB NOT NULL, password_enc BLOB NOT NULL,
                                      created_at INTEGER NOT NULL, last_used_at INTEGER NOT NULL, times_used INTEGER NOT NULL DEFAULT 0);
                 CREATE INDEX logins_by_origin ON logins(origin);
                 INSERT INTO collections VALUES (1, 'Recipes', 100);
                 INSERT INTO items VALUES (10, 1, 'Pasta', 'https://a.example/', 5000);
                 INSERT INTO item_tags VALUES (10, 'quick');
                 INSERT INTO logins VALUES (1, 'https://example.com', x'0102', x'0304', 1111, 2222, 3),
                                           (2, 'https://other.example', x'05', x'06', 4444, 4444, 0);
                 PRAGMA user_version = 3;",
            )
            .unwrap();
        }
        let s = Store::open(&path).unwrap();
        let version: i64 = s.conn().query_row("PRAGMA user_version", [], |r| r.get(0)).unwrap();
        assert_eq!(version, SCHEMA_VERSION);
        let mut logins = s.all_logins().unwrap();
        logins.sort_by_key(|l| l.id);
        assert_eq!(logins.len(), 2);
        assert_eq!((logins[0].created_at, logins[0].password_changed_at, logins[0].times_used), (1111, 1111, 3));
        assert_eq!(logins[0].password_enc, vec![3, 4]);
        assert_eq!(logins[1].password_changed_at, 4444);
        assert_eq!(s.items(None).unwrap()[0].tags, vec!["quick".to_string()]);
        // New logins and opening again both work.
        s.add_login("https://new.example", b"u", b"p").unwrap();
        drop(s);
        let s = Store::open(&path).unwrap();
        assert_eq!(s.login_count().unwrap(), 3);
        assert!(s.export_links_csv().unwrap().contains("Pasta,https://a.example/,Recipes,quick,1970-01-01T00:00:05Z\r\n"));
    }

    #[test]
    fn favourites_add_dup_remove_order_and_move() {
        let s = Store::open(&temp_path("favs")).unwrap();
        assert!(s.favorites().unwrap().is_empty());
        let a = s.add_favorite("https://a.example/", "A").unwrap();
        let b = s.add_favorite("https://b.example/", "B").unwrap();
        let c = s.add_favorite("https://c.example/", "C").unwrap();
        let order = |s: &Store| s.favorites().unwrap().into_iter().map(|f| f.id).collect::<Vec<_>>();
        assert_eq!(order(&s), vec![a.id, b.id, c.id]); // appended in the order added

        // Adding the same address again keeps its place; a new title replaces the old, a blank one doesn't.
        let a2 = s.add_favorite("https://a.example/", "A renamed").unwrap();
        assert_eq!((a2.id, a2.pos), (a.id, a.pos));
        s.add_favorite("https://a.example/", "  ").unwrap();
        assert_eq!(s.favorites().unwrap().len(), 3);
        assert_eq!(s.favorites().unwrap()[0].title, "A renamed");

        // Move right / left swaps with the neighbour.
        s.move_favorite(a.id, 1).unwrap();
        assert_eq!(order(&s), vec![b.id, a.id, c.id]);
        s.move_favorite(c.id, -1).unwrap();
        assert_eq!(order(&s), vec![b.id, c.id, a.id]);
        // At the ends nothing happens; an unknown id is ignored.
        s.move_favorite(b.id, -1).unwrap();
        s.move_favorite(a.id, 1).unwrap();
        s.move_favorite(9999, 1).unwrap();
        assert_eq!(order(&s), vec![b.id, c.id, a.id]);

        // Remove; adding again puts it at the end.
        s.remove_favorite(b.id).unwrap();
        assert_eq!(order(&s), vec![c.id, a.id]);
        let b3 = s.add_favorite("https://b.example/", "B").unwrap();
        assert_eq!(order(&s), vec![c.id, a.id, b3.id]);
    }

    #[test]
    fn site_host_matches_the_apps_host() {
        assert_eq!(site_host("https://WWW.Example.com:8080/a?b#c"), "example.com");
        assert_eq!(site_host("http://user@m.example.com/"), "m.example.com");
        assert_eq!(site_host("example.com"), "example.com");
        assert_eq!(site_host("http://10.0.2.2:8765/x"), "10.0.2.2");
        assert_eq!(site_host("http://[::1]:80/"), "[::1]");
    }

    #[test]
    fn favourite_icons_one_per_site_and_cleaned_up() {
        let s = Store::open(&temp_path("fav-icons")).unwrap();
        // No favourite on the site: nothing is stored (history and other pages stay letters).
        assert!(!s.set_favorite_icon("example.com", b"PNG0").unwrap());
        assert!(!s.favorite_icon_wanted("example.com", 1000).unwrap());
        assert!(s.favorite_icons().unwrap().is_empty());

        let a = s.add_favorite("https://www.example.com/", "Home").unwrap();
        let b = s.add_favorite("https://example.com/news", "News").unwrap();
        s.add_favorite("https://other.example/", "Other").unwrap();
        assert!(s.favorite_icon_wanted("www.example.com", 1000).unwrap());
        assert!(s.set_favorite_icon("www.Example.com", b"PNG1").unwrap());
        // Upsert: one row per site, shared by both favourites on it.
        assert!(s.set_favorite_icon("example.com", b"PNG2").unwrap());
        let icons = s.favorite_icons().unwrap();
        assert_eq!(icons.len(), 1);
        assert_eq!((icons[0].host.as_str(), icons[0].png.as_slice()), ("example.com", b"PNG2".as_slice()));
        // Fresh: not wanted again until it is older than the limit.
        assert!(!s.favorite_icon_wanted("example.com", 60_000).unwrap());
        s.conn().execute("UPDATE favorite_icons SET updated_at = 0", []).unwrap();
        assert!(s.favorite_icon_wanted("example.com", 60_000).unwrap());
        s.set_favorite_icon("other.example", b"PNG3").unwrap();

        // Removing one of two favourites on the site keeps the icon; removing the last drops it.
        s.remove_favorite(a.id).unwrap();
        assert_eq!(s.favorite_icons().unwrap().len(), 2);
        s.remove_favorite(b.id).unwrap();
        let hosts: Vec<String> = s.favorite_icons().unwrap().into_iter().map(|i| i.host).collect();
        assert_eq!(hosts, vec!["other.example".to_string()]);
    }

    #[test]
    fn favourites_are_their_own_list() {
        let s = Store::open(&temp_path("favs-own")).unwrap();
        // A favourite needn't be in the Vault, and deleting a Vault link leaves the favourite alone.
        let item = s.add_item(None, "Saved", "https://saved.example/", &[]).unwrap();
        s.add_favorite("https://saved.example/", "Saved").unwrap();
        s.add_favorite("https://home.example/", "Home page").unwrap();
        assert_eq!(s.item_count().unwrap(), 1);
        s.delete_item(item.id).unwrap();
        assert_eq!(s.favorites().unwrap().len(), 2);
        // Clearing history (part of every Flush) keeps them.
        s.record_visit("https://home.example/", "Home page", "home.example", 1).unwrap();
        s.set_favorite_icon("home.example", b"PNG").unwrap();
        s.clear_history().unwrap();
        assert_eq!(s.favorites().unwrap().len(), 2);
        assert_eq!(s.favorite_icons().unwrap().len(), 1);
    }

    #[test]
    fn upgrades_v4_database_adding_favourites_and_keeping_everything() {
        let path = temp_path("migrate-v4");
        {
            // A version-4 database exactly as release 0.2.2 wrote it.
            let c = Connection::open(&path).unwrap();
            c.execute_batch(
                "CREATE TABLE collections (id INTEGER PRIMARY KEY, name TEXT NOT NULL UNIQUE COLLATE NOCASE, created_at INTEGER NOT NULL);
                 CREATE TABLE items (id INTEGER PRIMARY KEY, collection_id INTEGER REFERENCES collections(id) ON DELETE SET NULL,
                                     title TEXT NOT NULL, url TEXT NOT NULL, created_at INTEGER NOT NULL);
                 CREATE INDEX items_by_collection ON items(collection_id, created_at DESC);
                 CREATE TABLE item_tags (item_id INTEGER NOT NULL REFERENCES items(id) ON DELETE CASCADE, tag TEXT NOT NULL, PRIMARY KEY (item_id, tag));
                 CREATE TABLE kv (key TEXT PRIMARY KEY, value TEXT NOT NULL);
                 CREATE TABLE history_entries (id INTEGER PRIMARY KEY, url TEXT NOT NULL UNIQUE, host TEXT NOT NULL, title TEXT NOT NULL, last_visit INTEGER NOT NULL);
                 CREATE TABLE history_visits (id INTEGER PRIMARY KEY, entry_id INTEGER NOT NULL REFERENCES history_entries(id) ON DELETE CASCADE, visited_at INTEGER NOT NULL);
                 CREATE TABLE logins (id INTEGER PRIMARY KEY, origin TEXT NOT NULL, username_enc BLOB NOT NULL, password_enc BLOB NOT NULL,
                                      created_at INTEGER NOT NULL, last_used_at INTEGER NOT NULL, times_used INTEGER NOT NULL DEFAULT 0,
                                      password_changed_at INTEGER NOT NULL DEFAULT 0);
                 CREATE INDEX logins_by_origin ON logins(origin);
                 INSERT INTO collections VALUES (1, 'Recipes', 100);
                 INSERT INTO items VALUES (10, 1, 'Pasta', 'https://a.example/', 5000), (11, NULL, 'Loose', 'https://b.example/', 6000);
                 INSERT INTO item_tags VALUES (10, 'quick');
                 INSERT INTO kv VALUES ('keep_list', '[\"mail.example.com\"]');
                 INSERT INTO history_entries VALUES (1, 'https://h.example/', 'h.example', 'H', 7);
                 INSERT INTO history_visits VALUES (1, 1, 7);
                 INSERT INTO logins VALUES (1, 'https://example.com', x'0102', x'0304', 1111, 2222, 3, 1500);
                 PRAGMA user_version = 4;",
            )
            .unwrap();
        }
        let s = Store::open(&path).unwrap();
        let version: i64 = s.conn().query_row("PRAGMA user_version", [], |r| r.get(0)).unwrap();
        assert_eq!(version, 5);
        // Everything kept, `items` unchanged; no favourites yet.
        let all = s.items(None).unwrap();
        assert_eq!(all.len(), 2);
        let pasta = all.iter().find(|i| i.id == 10).unwrap();
        assert_eq!((pasta.collection_id, pasta.tags.clone()), (Some(1), vec!["quick".to_string()]));
        let item_columns: i64 = s.conn().query_row("SELECT count(*) FROM pragma_table_info('items')", [], |r| r.get(0)).unwrap();
        assert_eq!(item_columns, 5);
        assert_eq!(s.collections().unwrap()[0].item_count, 1);
        assert_eq!(s.kv_get("keep_list").unwrap().as_deref(), Some("[\"mail.example.com\"]"));
        assert_eq!(s.history(None, 10).unwrap().len(), 1);
        let l = &s.all_logins().unwrap()[0];
        assert_eq!((l.password_enc.clone(), l.password_changed_at, l.times_used), (vec![3, 4], 1500, 3));
        assert!(s.favorites().unwrap().is_empty());
        assert!(s.favorite_icons().unwrap().is_empty());
        // Favourites work on the upgraded database and survive reopening.
        s.add_favorite("https://b.example/", "Loose").unwrap();
        s.add_favorite("https://home.example/", "Home").unwrap();
        s.set_favorite_icon("home.example", b"PNG").unwrap();
        drop(s);
        let s = Store::open(&path).unwrap();
        let urls: Vec<String> = s.favorites().unwrap().into_iter().map(|f| f.url).collect();
        assert_eq!(urls, vec!["https://b.example/".to_string(), "https://home.example/".to_string()]);
        assert_eq!(s.favorite_icons().unwrap().len(), 1);
        assert_eq!(s.items(None).unwrap().len(), 2);
    }

    #[test]
    fn store_exports_every_link() {
        let s = Store::open(&temp_path("export")).unwrap();
        let r = s.create_collection("Recipes").unwrap();
        s.add_item(Some(r.id), "Quiet pasta, \"fast\"", "https://a.example/", &["pasta".into()]).unwrap();
        s.add_item(None, "Loose", "https://b.example/", &[]).unwrap();
        let html = s.export_links_html().unwrap();
        assert!(html.contains(">Recipes</H3>"));
        assert!(html.contains("TAGS=\"pasta\">Quiet pasta, &quot;fast&quot;</A>"));
        assert!(html.contains(">Loose</A>"));
        let csv = s.export_links_csv().unwrap();
        assert_eq!(csv.matches("\r\n").count(), 3);
        assert!(csv.contains("\"Quiet pasta, \"\"fast\"\"\",https://a.example/,Recipes,pasta,"));
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
        assert_eq!(version, SCHEMA_VERSION);
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
