// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

//! corn-vault: Corn's local database (SQLite).
//!
//! Holds everything Corn keeps that could reveal browsing:
//! - the **Vault**: Vault Items grouped into Collections, with optional tags
//! - a small key-value area for the Keep List, per-site blocking choices
//!   and the Kept Session
//!
//! The file lives in the app's private storage, protected by the OS sandbox
//! and the phone's own encryption; the Vault lock gates access in the UI.

use std::sync::Mutex;
use std::time::{SystemTime, UNIX_EPOCH};

use rusqlite::{params, Connection, OptionalExtension};

/// The Collection a Vault Item lands in when the user doesn't pick one.
pub const UNSORTED: &str = "Unsorted";

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
    pub collection_id: i64,
    pub title: String,
    pub url: String,
    pub tags: Vec<String>,
    pub created_at: i64,
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
    collection_id INTEGER NOT NULL REFERENCES collections(id) ON DELETE CASCADE,
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
";

fn now() -> i64 {
    SystemTime::now().duration_since(UNIX_EPOCH).map(|d| d.as_millis() as i64).unwrap_or(0)
}

impl Store {
    /// Open (or create) the database at `path`.
    pub fn open(path: &str) -> Result<Self> {
        let conn = Connection::open(path)?;
        conn.execute_batch("PRAGMA foreign_keys = ON; PRAGMA journal_mode = WAL;")?;
        conn.execute_batch(SCHEMA)?;
        conn.execute(
            "INSERT OR IGNORE INTO collections (name, created_at) VALUES (?1, ?2)",
            params![UNSORTED, 0],
        )?;
        Ok(Self { conn: Mutex::new(conn) })
    }

    fn conn(&self) -> std::sync::MutexGuard<'_, Connection> {
        self.conn.lock().unwrap_or_else(|e| e.into_inner())
    }

    pub fn unsorted_id(&self) -> Result<i64> {
        Ok(self.conn().query_row("SELECT id FROM collections WHERE name = ?1", [UNSORTED], |r| r.get(0))?)
    }

    /// Collections with item counts. Unsorted first, then by creation order.
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

    /// Find a collection by name, creating it if needed (used by "Save group to Vault").
    pub fn collection_named(&self, name: &str) -> Result<Collection> {
        let found = self.collections()?.into_iter().find(|c| c.name.eq_ignore_ascii_case(name.trim()));
        match found {
            Some(c) => Ok(c),
            None => self.create_collection(name),
        }
    }

    pub fn add_item(&self, collection_id: i64, title: &str, url: &str, tags: &[String]) -> Result<VaultItem> {
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

    /// True if any Vault Item points at exactly this URL.
    pub fn contains_url(&self, url: &str) -> Result<bool> {
        Ok(self.conn().query_row("SELECT EXISTS(SELECT 1 FROM items WHERE url = ?1)", [url], |r| r.get(0))?)
    }

    pub fn items(&self, collection_id: i64) -> Result<Vec<VaultItem>> {
        self.query_items("WHERE i.collection_id = ?1", params![collection_id])
    }

    /// Newest items across every Collection.
    pub fn recent_items(&self, limit: i64) -> Result<Vec<VaultItem>> {
        let sql = format!("WHERE 1 = 1 LIMIT {}", limit.max(0));
        self.query_items(&sql, params![])
    }

    /// Case-insensitive search across titles, URLs and tags.
    pub fn search(&self, query: &str) -> Result<Vec<VaultItem>> {
        let like = format!("%{}%", query.trim().replace('%', "\\%").replace('_', "\\_"));
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

#[cfg(test)]
mod tests {
    use super::*;

    fn temp_path(name: &str) -> String {
        let dir = std::env::temp_dir().join(format!("corn-vault-test-{}-{}", std::process::id(), name));
        let _ = std::fs::remove_file(&dir);
        dir.to_string_lossy().into_owned()
    }

    #[test]
    fn unsorted_exists_on_first_open() {
        let s = Store::open(&temp_path("unsorted")).unwrap();
        let cs = s.collections().unwrap();
        assert_eq!(cs.len(), 1);
        assert_eq!(cs[0].name, UNSORTED);
    }

    #[test]
    fn save_find_and_count() {
        let s = Store::open(&temp_path("save")).unwrap();
        let later = s.create_collection("Watch later").unwrap();
        s.add_item(later.id, "Rainy window ambience", "https://example.com/v/3", &["Chill".into(), "sleep".into(), "chill".into()]).unwrap();
        s.add_item(s.unsorted_id().unwrap(), "Ten snacks", "https://example.com/a/3", &[]).unwrap();

        let items = s.items(later.id).unwrap();
        assert_eq!(items.len(), 1);
        assert_eq!(items[0].tags, vec!["chill".to_string(), "sleep".to_string()]);
        assert_eq!(s.collections().unwrap().iter().find(|c| c.id == later.id).unwrap().item_count, 1);
        assert!(s.contains_url("https://example.com/a/3").unwrap());
        assert_eq!(s.search("SLEEP").unwrap().len(), 1);
        assert_eq!(s.search("snacks").unwrap().len(), 1);
        assert_eq!(s.recent_items(1).unwrap().len(), 1);
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
        assert_eq!(s.kv_get("keep_list").unwrap(), None);
        s.kv_set("keep_list", "[\"mail.example.com\"]").unwrap();
        s.kv_set("keep_list", "[]").unwrap();
        assert_eq!(s.kv_get("keep_list").unwrap().as_deref(), Some("[]"));
        s.kv_delete("keep_list").unwrap();
        assert_eq!(s.kv_get("keep_list").unwrap(), None);
    }

    #[test]
    fn data_survives_reopen() {
        let path = temp_path("reopen");
        {
            let s = Store::open(&path).unwrap();
            s.add_item(s.unsorted_id().unwrap(), "kept", "https://example.com/", &[]).unwrap();
        }
        let s = Store::open(&path).unwrap();
        assert_eq!(s.collections().unwrap().len(), 1);
        assert!(s.contains_url("https://example.com/").unwrap());
    }
}
