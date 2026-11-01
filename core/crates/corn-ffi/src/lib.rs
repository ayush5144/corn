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
