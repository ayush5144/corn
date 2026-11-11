// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

//! flush-ffi: the only crate the apps link against. Exposes Corn's Rust core
//! to Kotlin (Android) and Swift (iOS) through UniFFI.
//!
//! Hot-path rule: `CoreBlocker::check` is called for every sub-request, so it
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
pub enum CoreError {
    #[error("{msg}")]
    EngineLoad { msg: String },
    #[error("{msg}")]
    Store { msg: String },
}

impl From<flush_store::VaultError> for CoreError {
    fn from(e: flush_store::VaultError) -> Self {
        CoreError::Store { msg: e.to_string() }
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
    flush_block::preprocess::preprocess(&text).includes
}

/// The relative paths one uBlock Origin scriptlet module imports (`./x.js`, `../y.js`).
#[uniffi::export]
pub fn scriptlet_module_imports(source: String) -> Vec<String> {
    flush_block::scriptlets::module_imports(&source)
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
pub fn assemble_scriptlets(sources: Vec<String>) -> Result<ScriptletSet, CoreError> {
    let resources = flush_block::scriptlets::assemble(&sources).map_err(|e| CoreError::EngineLoad { msg: e.to_string() })?;
    Ok(ScriptletSet { json: flush_block::scriptlets::to_json(&resources), count: resources.len() as u32 })
}

fn parse_scriptlets(json: Option<String>) -> Result<Vec<flush_block::Resource>, CoreError> {
    match json {
        None => Ok(Vec::new()),
        Some(j) => flush_block::scriptlets::from_json(&j).map_err(|e| CoreError::EngineLoad { msg: e.to_string() }),
    }
}

#[derive(uniffi::Object)]
pub struct CoreBlocker {
    inner: flush_block::Blocker,
}

#[uniffi::export]
impl CoreBlocker {
    /// Parse filter-list texts into a new engine. Slow: run off the main thread, then cache with `serialize`.
    #[uniffi::constructor]
    pub fn from_lists(lists: Vec<String>) -> Arc<Self> {
        Arc::new(Self { inner: flush_block::Blocker::from_lists(&lists) })
    }

    /// Parse downloaded filter lists (with their `!#include`d files as separate entries) into a new
    /// engine that can run `+js(...)` scriptlets. `scriptlets_json` comes from
    /// `assemble_scriptlets`; None = no scriptlets. Slow: run off the main thread.
    #[uniffi::constructor]
    pub fn from_filter_lists(lists: Vec<FilterList>, scriptlets_json: Option<String>) -> Result<Arc<Self>, CoreError> {
        let resources = parse_scriptlets(scriptlets_json)?;
        let lists: Vec<flush_block::FilterList<String>> =
            lists.into_iter().map(|l| flush_block::FilterList { text: l.text, trusted: l.trusted }).collect();
        Ok(Arc::new(Self { inner: flush_block::Blocker::from_filter_lists(&lists, resources) }))
    }

    /// Load a cached engine produced by `serialize`.
    #[uniffi::constructor]
    pub fn from_serialized(bytes: Vec<u8>) -> Result<Arc<Self>, CoreError> {
        Self::from_serialized_with_scriptlets(bytes, None)
    }

    /// Load a cached engine plus its scriptlets (they aren't part of the saved engine).
    #[uniffi::constructor]
    pub fn from_serialized_with_scriptlets(bytes: Vec<u8>, scriptlets_json: Option<String>) -> Result<Arc<Self>, CoreError> {
        let resources = parse_scriptlets(scriptlets_json)?;
        flush_block::Blocker::from_serialized(&bytes, resources)
            .map(|inner| Arc::new(Self { inner }))
            .map_err(|e| CoreError::EngineLoad { msg: e.to_string() })
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
            flush_block::Decision::Allow => Decision::Allow,
            flush_block::Decision::Block => Decision::Block,
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
}

impl From<flush_store::Login> for Login {
    fn from(l: flush_store::Login) -> Self {
        Self {
            id: l.id,
            origin: l.origin,
            username_enc: l.username_enc,
            password_enc: l.password_enc,
            created_at: l.created_at,
            last_used_at: l.last_used_at,
            times_used: l.times_used,
        }
    }
}

impl From<flush_store::Collection> for Collection {
    fn from(c: flush_store::Collection) -> Self {
        Self { id: c.id, name: c.name, item_count: c.item_count }
    }
}

impl From<flush_store::VaultItem> for VaultItem {
    fn from(i: flush_store::VaultItem) -> Self {
        Self { id: i.id, collection_id: i.collection_id, title: i.title, url: i.url, tags: i.tags, created_at: i.created_at }
    }
}

impl From<flush_store::HistoryVisit> for HistoryVisit {
    fn from(v: flush_store::HistoryVisit) -> Self {
        Self { visit_id: v.visit_id, url: v.url, title: v.title, host: v.host, visited_at: v.visited_at }
    }
}

#[derive(uniffi::Object)]
pub struct CoreStore {
    inner: flush_store::Store,
}

#[uniffi::export]
impl CoreStore {
    /// Open (create or upgrade) the database file in the app's private storage.
    #[uniffi::constructor]
    pub fn open(path: String) -> Result<Arc<Self>, CoreError> {
        Ok(Arc::new(Self { inner: flush_store::Store::open(&path)? }))
    }

    // Vault
    pub fn collections(&self) -> Result<Vec<Collection>, CoreError> {
        Ok(self.inner.collections()?.into_iter().map(Into::into).collect())
    }
    pub fn create_collection(&self, name: String) -> Result<Collection, CoreError> {
        Ok(self.inner.create_collection(&name)?.into())
    }
    pub fn collection_named(&self, name: String) -> Result<Collection, CoreError> {
        Ok(self.inner.collection_named(&name)?.into())
    }
    pub fn item_count(&self) -> Result<i64, CoreError> {
        Ok(self.inner.item_count()?)
    }
    pub fn add_item(&self, collection_id: Option<i64>, title: String, url: String, tags: Vec<String>) -> Result<VaultItem, CoreError> {
        Ok(self.inner.add_item(collection_id, &title, &url, &tags)?.into())
    }
    pub fn contains_url(&self, url: String) -> Result<bool, CoreError> {
        Ok(self.inner.contains_url(&url)?)
    }
    /// Items in one Collection, or every saved link (All) when `collection_id` is None.
    pub fn items(&self, collection_id: Option<i64>) -> Result<Vec<VaultItem>, CoreError> {
        Ok(self.inner.items(collection_id)?.into_iter().map(Into::into).collect())
    }
    pub fn recent_items(&self, limit: i64) -> Result<Vec<VaultItem>, CoreError> {
        Ok(self.inner.recent_items(limit)?.into_iter().map(Into::into).collect())
    }
    pub fn search(&self, query: String) -> Result<Vec<VaultItem>, CoreError> {
        Ok(self.inner.search(&query)?.into_iter().map(Into::into).collect())
    }
    pub fn delete_item(&self, id: i64) -> Result<(), CoreError> {
        Ok(self.inner.delete_item(id)?)
    }

    // History
    pub fn record_visit(&self, url: String, title: String, host: String, at: i64) -> Result<(), CoreError> {
        Ok(self.inner.record_visit(&url, &title, &host, at)?)
    }
    pub fn update_title(&self, url: String, title: String) -> Result<(), CoreError> {
        Ok(self.inner.update_title(&url, &title)?)
    }
    pub fn history(&self, query: Option<String>, limit: i64) -> Result<Vec<HistoryVisit>, CoreError> {
        Ok(self.inner.history(query.as_deref(), limit)?.into_iter().map(Into::into).collect())
    }
    pub fn delete_visit(&self, visit_id: i64) -> Result<(), CoreError> {
        Ok(self.inner.delete_visit(visit_id)?)
    }
    pub fn delete_history_for_host(&self, host: String) -> Result<(), CoreError> {
        Ok(self.inner.delete_history_for_host(&host)?)
    }
    pub fn clear_history(&self) -> Result<(), CoreError> {
        Ok(self.inner.clear_history()?)
    }

    // Passwords (blobs encrypted by the app)
    pub fn add_login(&self, origin: String, username_enc: Vec<u8>, password_enc: Vec<u8>) -> Result<Login, CoreError> {
        Ok(self.inner.add_login(&origin, &username_enc, &password_enc)?.into())
    }
    pub fn update_login(&self, id: i64, username_enc: Vec<u8>, password_enc: Vec<u8>) -> Result<(), CoreError> {
        Ok(self.inner.update_login(id, &username_enc, &password_enc)?)
    }
    pub fn delete_login(&self, id: i64) -> Result<(), CoreError> {
        Ok(self.inner.delete_login(id)?)
    }
    pub fn delete_all_logins(&self) -> Result<(), CoreError> {
        Ok(self.inner.delete_all_logins()?)
    }
    pub fn logins_for_origin(&self, origin: String) -> Result<Vec<Login>, CoreError> {
        Ok(self.inner.logins_for_origin(&origin)?.into_iter().map(Into::into).collect())
    }
    pub fn all_logins(&self) -> Result<Vec<Login>, CoreError> {
        Ok(self.inner.all_logins()?.into_iter().map(Into::into).collect())
    }
    pub fn login_count(&self) -> Result<i64, CoreError> {
        Ok(self.inner.login_count()?)
    }
    pub fn touch_login(&self, id: i64) -> Result<(), CoreError> {
        Ok(self.inner.touch_login(id)?)
    }

    // Key-value
    pub fn kv_get(&self, key: String) -> Result<Option<String>, CoreError> {
        Ok(self.inner.kv_get(&key)?)
    }
    pub fn kv_set(&self, key: String, value: String) -> Result<(), CoreError> {
        Ok(self.inner.kv_set(&key, &value)?)
    }
    pub fn kv_delete(&self, key: String) -> Result<(), CoreError> {
        Ok(self.inner.kv_delete(&key)?)
    }
}

// ---------------- URL hygiene ----------------

/// Remove well-known tracking parameters (utm_*, fbclid, gclid…) from a link.
#[uniffi::export]
pub fn clean_url(url: String) -> String {
    flush_url::clean(&url)
}

/// The https version of an http URL, or None when it's already secure or local.
#[uniffi::export]
pub fn https_upgrade(url: String) -> Option<String> {
    flush_url::https_upgrade(&url)
}
