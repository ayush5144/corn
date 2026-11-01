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

    /// Load a cached engine produced by `serialize`.
    #[uniffi::constructor]
    pub fn from_serialized(bytes: Vec<u8>) -> Result<Arc<Self>, CornError> {
        corn_block::Blocker::from_serialized(&bytes)
            .map(|inner| Arc::new(Self { inner }))
            .map_err(|e| CornError::EngineLoad { msg: e.to_string() })
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

// ---------------- Local store (Vault, Keep List, session) ----------------

#[derive(Debug, Clone, uniffi::Record)]
pub struct Collection {
    pub id: i64,
    pub name: String,
    pub item_count: i64,
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct VaultItem {
    pub id: i64,
    pub collection_id: i64,
    pub title: String,
    pub url: String,
    pub tags: Vec<String>,
    pub created_at: i64,
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

#[derive(uniffi::Object)]
pub struct CornStore {
    inner: corn_vault::Store,
}

#[uniffi::export]
impl CornStore {
    /// Open (or create) the database file in the app's private storage.
    #[uniffi::constructor]
    pub fn open(path: String) -> Result<Arc<Self>, CornError> {
        Ok(Arc::new(Self { inner: corn_vault::Store::open(&path)? }))
    }

    pub fn unsorted_id(&self) -> Result<i64, CornError> {
        Ok(self.inner.unsorted_id()?)
    }

    pub fn collections(&self) -> Result<Vec<Collection>, CornError> {
        Ok(self.inner.collections()?.into_iter().map(Into::into).collect())
    }

    pub fn create_collection(&self, name: String) -> Result<Collection, CornError> {
        Ok(self.inner.create_collection(&name)?.into())
    }

    pub fn collection_named(&self, name: String) -> Result<Collection, CornError> {
        Ok(self.inner.collection_named(&name)?.into())
    }

    pub fn add_item(&self, collection_id: i64, title: String, url: String, tags: Vec<String>) -> Result<VaultItem, CornError> {
        Ok(self.inner.add_item(collection_id, &title, &url, &tags)?.into())
    }

    pub fn contains_url(&self, url: String) -> Result<bool, CornError> {
        Ok(self.inner.contains_url(&url)?)
    }

    pub fn items(&self, collection_id: i64) -> Result<Vec<VaultItem>, CornError> {
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
