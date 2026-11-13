// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

//! corn-ffi: the only crate the apps link against. Exposes Corn's Rust core
//! to Kotlin (Android) and Swift (iOS) through UniFFI.
//!
//! Hot-path rule: `CornBlocker::check` is called for every sub-request, so it
//! takes plain strings and returns a small enum.

use std::sync::Arc;

uniffi::setup_scaffolding!();

#[derive(Debug, Clone, PartialEq, Eq, uniffi::Enum)]
pub enum Decision {
    Allow,
    Block,
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct Cosmetic {
    pub hide_css: String,
    pub injected_script: String,
    pub generichide: bool,
    pub exceptions: Vec<String>,
}

#[derive(Debug, thiserror::Error, uniffi::Error)]
pub enum CornError {
    #[error("{msg}")]
    EngineLoad { msg: String },
    #[error("{msg}")]
    Store { msg: String },
}

impl From<corn_vault::VaultError> for CornError {
    fn from(e: corn_vault::VaultError) -> Self {
        CornError::Store { msg: e.to_string() }
    }
}

/// One downloaded filter list. Trusted lists (uBlock Origin's own) may use uBO's `trusted-*` scriptlets.
#[derive(Debug, Clone, uniffi::Record)]
pub struct FilterList {
    pub text: String,
    pub trusted: bool,
}

/// What a list asks for after its `!#if` blocks are resolved: the `!#include`d file names
/// (plain names in the list's own folder). The app downloads them and passes them as more lists.
#[uniffi::export]
pub fn filter_list_includes(text: String) -> Vec<String> {
    corn_block::preprocess::preprocess(&text).includes
}

/// The relative paths one uBlock Origin scriptlet module imports (`./x.js`, `../y.js`).
#[uniffi::export]
pub fn scriptlet_module_imports(source: String) -> Vec<String> {
    corn_block::scriptlets::module_imports(&source)
}

/// Scriptlet resources assembled from uBO's module files.
#[derive(Debug, Clone, uniffi::Record)]
pub struct ScriptletSet {
    /// adblock-rust's resources JSON; store it and pass it to the engine constructors.
    pub json: String,
    pub count: u32,
}

/// Turn the text of all uBO scriptlet modules into resources. Fails (keep the last good set)
/// if the files don't look like a complete uBO release.
#[uniffi::export]
pub fn assemble_scriptlets(sources: Vec<String>) -> Result<ScriptletSet, CornError> {
    let resources = corn_block::scriptlets::assemble(&sources).map_err(|e| CornError::EngineLoad { msg: e.to_string() })?;
    Ok(ScriptletSet { json: corn_block::scriptlets::to_json(&resources), count: resources.len() as u32 })
}

fn parse_scriptlets(json: Option<String>) -> Result<Vec<corn_block::Resource>, CornError> {
    match json {
        None => Ok(Vec::new()),
        Some(j) => corn_block::scriptlets::from_json(&j).map_err(|e| CornError::EngineLoad { msg: e.to_string() }),
    }
}

#[derive(uniffi::Object)]
pub struct CornBlocker {
    inner: corn_block::Blocker,
}

#[uniffi::export]
impl CornBlocker {
    /// Parse filter-list texts into a new engine. Slow: run off the main thread, then cache with `serialize`.
    #[uniffi::constructor]
    pub fn from_lists(lists: Vec<String>) -> Arc<Self> {
        Arc::new(Self { inner: corn_block::Blocker::from_lists(&lists) })
    }

    /// Parse downloaded filter lists (with their `!#include`d files as separate entries) into a new
    /// engine that can run `+js(...)` scriptlets. `scriptlets_json` comes from
    /// `assemble_scriptlets`; None = no scriptlets. Slow: run off the main thread.
    #[uniffi::constructor]
    pub fn from_filter_lists(lists: Vec<FilterList>, scriptlets_json: Option<String>) -> Result<Arc<Self>, CornError> {
        let resources = parse_scriptlets(scriptlets_json)?;
        let lists: Vec<corn_block::FilterList<String>> =
            lists.into_iter().map(|l| corn_block::FilterList { text: l.text, trusted: l.trusted }).collect();
        Ok(Arc::new(Self { inner: corn_block::Blocker::from_filter_lists(&lists, resources) }))
    }

    /// Load a cached engine produced by `serialize`.
    #[uniffi::constructor]
    pub fn from_serialized(bytes: Vec<u8>) -> Result<Arc<Self>, CornError> {
        Self::from_serialized_with_scriptlets(bytes, None)
    }

    /// Load a cached engine plus its scriptlets (they aren't part of the saved engine).
    #[uniffi::constructor]
    pub fn from_serialized_with_scriptlets(bytes: Vec<u8>, scriptlets_json: Option<String>) -> Result<Arc<Self>, CornError> {
        let resources = parse_scriptlets(scriptlets_json)?;
        corn_block::Blocker::from_serialized(&bytes, resources)
            .map(|inner| Arc::new(Self { inner }))
            .map_err(|e| CornError::EngineLoad { msg: e.to_string() })
    }

    /// The scriptlets to run at document start on the page at `url` (empty if none).
    /// Depends only on the host name, so callers may cache it per host.
    pub fn scriptlets(&self, url: String) -> String {
        self.inner.scriptlets(&url)
    }

    pub fn serialize(&self) -> Vec<u8> {
        self.inner.serialize()
    }

    pub fn check(&self, url: String, source_url: String, accept: Option<String>, is_main_frame: bool) -> Decision {
        match self.inner.check(&url, &source_url, accept.as_deref(), is_main_frame) {
            corn_block::Decision::Allow => Decision::Allow,
            corn_block::Decision::Block => Decision::Block,
        }
    }

    pub fn cosmetic(&self, url: String) -> Cosmetic {
        let c = self.inner.cosmetic(&url);
        Cosmetic {
            hide_css: c.hide_css,
            injected_script: c.injected_script,
            generichide: c.generichide,
            exceptions: c.exceptions,
        }
    }

    pub fn generic_css(&self, classes: Vec<String>, ids: Vec<String>, exceptions: Vec<String>) -> String {
        self.inner.generic_css(&classes, &ids, &exceptions)
    }
}

// ---------------- Local store (Vault, history, Keep List, session) ----------------

#[derive(Debug, Clone, uniffi::Record)]
pub struct Collection {
    pub id: i64,
    pub name: String,
    pub item_count: i64,
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct VaultItem {
    pub id: i64,
    /// None = in no Collection (shown only in All).
    pub collection_id: Option<i64>,
    pub title: String,
    pub url: String,
    pub tags: Vec<String>,
    pub created_at: i64,
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct HistoryVisit {
    pub visit_id: i64,
    pub url: String,
    pub title: String,
    pub host: String,
    pub visited_at: i64,
}

/// A saved login. The username and password are encrypted by the app (AES-256-GCM); Rust only stores the blobs.
#[derive(Debug, Clone, uniffi::Record)]
pub struct Login {
    pub id: i64,
    /// `https://host[:port]`: the exact site the login belongs to.
    pub origin: String,
    pub username_enc: Vec<u8>,
    pub password_enc: Vec<u8>,
    pub created_at: i64,
    pub last_used_at: i64,
    pub times_used: i64,
    /// When the password was last saved or changed (milliseconds).
    pub password_changed_at: i64,
}

impl From<corn_vault::Login> for Login {
    fn from(l: corn_vault::Login) -> Self {
        Self {
            id: l.id,
            origin: l.origin,
            username_enc: l.username_enc,
            password_enc: l.password_enc,
            created_at: l.created_at,
            last_used_at: l.last_used_at,
            times_used: l.times_used,
            password_changed_at: l.password_changed_at,
        }
    }
}

impl From<corn_vault::Collection> for Collection {
    fn from(c: corn_vault::Collection) -> Self {
        Self { id: c.id, name: c.name, item_count: c.item_count }
    }
}

impl From<corn_vault::VaultItem> for VaultItem {
    fn from(i: corn_vault::VaultItem) -> Self {
        Self { id: i.id, collection_id: i.collection_id, title: i.title, url: i.url, tags: i.tags, created_at: i.created_at }
    }
}

impl From<corn_vault::HistoryVisit> for HistoryVisit {
    fn from(v: corn_vault::HistoryVisit) -> Self {
        Self { visit_id: v.visit_id, url: v.url, title: v.title, host: v.host, visited_at: v.visited_at }
    }
}

#[derive(uniffi::Object)]
pub struct CornStore {
    inner: corn_vault::Store,
}

#[uniffi::export]
impl CornStore {
    /// Open (create or upgrade) the database file in the app's private storage.
    #[uniffi::constructor]
    pub fn open(path: String) -> Result<Arc<Self>, CornError> {
        Ok(Arc::new(Self { inner: corn_vault::Store::open(&path)? }))
    }

    // Vault
    pub fn collections(&self) -> Result<Vec<Collection>, CornError> {
        Ok(self.inner.collections()?.into_iter().map(Into::into).collect())
    }
    pub fn create_collection(&self, name: String) -> Result<Collection, CornError> {
        Ok(self.inner.create_collection(&name)?.into())
    }
    pub fn collection_named(&self, name: String) -> Result<Collection, CornError> {
        Ok(self.inner.collection_named(&name)?.into())
    }
    pub fn item_count(&self) -> Result<i64, CornError> {
        Ok(self.inner.item_count()?)
    }
    pub fn add_item(&self, collection_id: Option<i64>, title: String, url: String, tags: Vec<String>) -> Result<VaultItem, CornError> {
        Ok(self.inner.add_item(collection_id, &title, &url, &tags)?.into())
    }
    pub fn contains_url(&self, url: String) -> Result<bool, CornError> {
        Ok(self.inner.contains_url(&url)?)
    }
    /// Items in one Collection, or every saved link (All) when `collection_id` is None.
    pub fn items(&self, collection_id: Option<i64>) -> Result<Vec<VaultItem>, CornError> {
        Ok(self.inner.items(collection_id)?.into_iter().map(Into::into).collect())
    }
    pub fn recent_items(&self, limit: i64) -> Result<Vec<VaultItem>, CornError> {
        Ok(self.inner.recent_items(limit)?.into_iter().map(Into::into).collect())
    }
    pub fn search(&self, query: String) -> Result<Vec<VaultItem>, CornError> {
        Ok(self.inner.search(&query)?.into_iter().map(Into::into).collect())
    }
    pub fn delete_item(&self, id: i64) -> Result<(), CornError> {
        Ok(self.inner.delete_item(id)?)
    }

    // History
    pub fn record_visit(&self, url: String, title: String, host: String, at: i64) -> Result<(), CornError> {
        Ok(self.inner.record_visit(&url, &title, &host, at)?)
    }
    pub fn update_title(&self, url: String, title: String) -> Result<(), CornError> {
        Ok(self.inner.update_title(&url, &title)?)
    }
    pub fn history(&self, query: Option<String>, limit: i64) -> Result<Vec<HistoryVisit>, CornError> {
        Ok(self.inner.history(query.as_deref(), limit)?.into_iter().map(Into::into).collect())
    }
    pub fn delete_visit(&self, visit_id: i64) -> Result<(), CornError> {
        Ok(self.inner.delete_visit(visit_id)?)
    }
    pub fn delete_history_for_host(&self, host: String) -> Result<(), CornError> {
        Ok(self.inner.delete_history_for_host(&host)?)
    }
    pub fn clear_history(&self) -> Result<(), CornError> {
        Ok(self.inner.clear_history()?)
    }

    // Passwords (blobs encrypted by the app)
    pub fn add_login(&self, origin: String, username_enc: Vec<u8>, password_enc: Vec<u8>) -> Result<Login, CornError> {
        Ok(self.inner.add_login(&origin, &username_enc, &password_enc)?.into())
    }
    /// `password_changed`: the password text changed (not just the username), so its date moves.
    pub fn update_login(&self, id: i64, username_enc: Vec<u8>, password_enc: Vec<u8>, password_changed: bool) -> Result<(), CornError> {
        Ok(self.inner.update_login(id, &username_enc, &password_enc, password_changed)?)
    }
    pub fn delete_login(&self, id: i64) -> Result<(), CornError> {
        Ok(self.inner.delete_login(id)?)
    }
    pub fn delete_all_logins(&self) -> Result<(), CornError> {
        Ok(self.inner.delete_all_logins()?)
    }
    pub fn logins_for_origin(&self, origin: String) -> Result<Vec<Login>, CornError> {
        Ok(self.inner.logins_for_origin(&origin)?.into_iter().map(Into::into).collect())
    }
    pub fn all_logins(&self) -> Result<Vec<Login>, CornError> {
        Ok(self.inner.all_logins()?.into_iter().map(Into::into).collect())
    }
    pub fn login_count(&self) -> Result<i64, CornError> {
        Ok(self.inner.login_count()?)
    }
    pub fn touch_login(&self, id: i64) -> Result<(), CornError> {
        Ok(self.inner.touch_login(id)?)
    }

    // Export (docs/02-technical/13-vault-export.md)
    /// Every saved link as an HTML bookmarks file.
    pub fn export_links_html(&self) -> Result<String, CornError> {
        Ok(self.inner.export_links_html()?)
    }
    /// Every saved link as CSV.
    pub fn export_links_csv(&self) -> Result<String, CornError> {
        Ok(self.inner.export_links_csv()?)
    }

    // Key-value
    pub fn kv_get(&self, key: String) -> Result<Option<String>, CornError> {
        Ok(self.inner.kv_get(&key)?)
    }
    pub fn kv_set(&self, key: String, value: String) -> Result<(), CornError> {
        Ok(self.inner.kv_set(&key, &value)?)
    }
    pub fn kv_delete(&self, key: String) -> Result<(), CornError> {
        Ok(self.inner.kv_delete(&key)?)
    }
}

/// One saved login in clear, for the passwords export. Built by the app (which holds the key)
/// right before writing the file, and dropped right after.
#[derive(Debug, Clone, uniffi::Record)]
pub struct PasswordExportRow {
    pub origin: String,
    pub username: String,
    pub password: String,
}

/// Saved passwords as CSV in Chrome's format (`name,url,username,password,note`). Plain text:
/// the caller writes it only to the place the user chose.
#[uniffi::export]
pub fn export_passwords_csv(rows: Vec<PasswordExportRow>) -> String {
    let rows: Vec<corn_vault::export::PasswordRow> = rows
        .into_iter()
        .map(|r| corn_vault::export::PasswordRow { origin: r.origin, username: r.username, password: r.password })
        .collect();
    corn_vault::export::passwords_csv(&rows)
}

// ---------------- URL hygiene ----------------

/// Remove well-known tracking parameters (utm_*, fbclid, gclid…) from a link.
#[uniffi::export]
pub fn clean_url(url: String) -> String {
    corn_url::clean(&url)
}

/// The https version of an http URL, or None when it's already secure or local.
#[uniffi::export]
pub fn https_upgrade(url: String) -> Option<String> {
    corn_url::https_upgrade(&url)
}
