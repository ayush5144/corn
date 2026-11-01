// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

//! corn-block: Corn's wrapper around Brave's `adblock-rust` engine.
//!
//! One [`Blocker`] is built once (from filter-list text, or from a cached
//! serialized engine) and shared read-only by every tab.

use std::collections::HashSet;

use adblock::lists::{FilterSet, ParseOptions};
use adblock::request::Request;
use adblock::Engine;

/// What to do with a network request.
#[derive(Debug, Clone, PartialEq, Eq)]
pub enum Decision {
    Allow,
    Block,
}

/// Cosmetic (element-hiding) resources for one page.
#[derive(Debug, Clone, Default, PartialEq, Eq)]
pub struct Cosmetic {
    /// A ready-to-inject stylesheet hiding matched elements. Empty if nothing matched.
    pub hide_css: String,
    /// Scriptlet JavaScript to inject at document start. Empty if none.
    pub injected_script: String,
    /// When true, the page must not ask for generic class/id rules.
    pub generichide: bool,
    /// Pass these back to [`Blocker::generic_css`].
    pub exceptions: Vec<String>,
}

#[derive(Debug, thiserror::Error)]
pub enum BlockError {
    #[error("the cached engine could not be loaded: {0}")]
    Deserialize(String),
}

pub struct Blocker {
    engine: Engine,
}

impl Blocker {
    /// Build an engine from the text of one or more filter lists (EasyList, EasyPrivacy, …).
    pub fn from_lists<S: AsRef<str>>(lists: &[S]) -> Self {
        let mut set = FilterSet::new(false);
        for list in lists {
            set.add_filter_list(list.as_ref().to_owned(), ParseOptions::default());
        }
        Self { engine: Engine::new_with_filter_set(set) }
    }

    /// Load an engine previously produced by [`Blocker::serialize`]. Much faster than parsing lists.
    pub fn from_serialized(bytes: &[u8]) -> Result<Self, BlockError> {
        let mut engine = Engine::default();
        engine
            .deserialize(bytes)
            .map_err(|e| BlockError::Deserialize(format!("{e:?}")))?;
        Ok(Self { engine })
    }

    pub fn serialize(&self) -> Vec<u8> {
        self.engine.serialize()
    }

    /// Check one sub-request. `accept` is the request's Accept header, if known.
    pub fn check(&self, url: &str, source_url: &str, accept: Option<&str>, is_main_frame: bool) -> Decision {
        if is_main_frame {
            // Never block the page the user asked for.
            return Decision::Allow;
        }
        let kind = guess_request_type(url, accept);
        match Request::new(url, source_url, kind, "GET") {
            Ok(req) if self.engine.check_network_request(&req).should_block() => Decision::Block,
            _ => Decision::Allow,
        }
    }

    /// Element-hiding CSS and scriptlets for the page at `url`.
    pub fn cosmetic(&self, url: &str) -> Cosmetic {
        let res = self.engine.url_cosmetic_resources(url);
        let mut selectors: Vec<&String> = res.hide_selectors.iter().collect();
        selectors.sort();
        Cosmetic {
            hide_css: css_for(selectors.iter().map(|s| s.as_str())),
            injected_script: res.injected_script,
            generichide: res.generichide,
            exceptions: res.exceptions.into_iter().collect(),
        }
    }

    /// CSS for generic rules triggered by the classes and ids present on a page.
    pub fn generic_css(&self, classes: &[String], ids: &[String], exceptions: &[String]) -> String {
        let exceptions: HashSet<String> = exceptions.iter().cloned().collect();
        let selectors = self.engine.hidden_class_id_selectors(classes, ids, &exceptions);
        css_for(selectors.iter().map(|s| s.as_str()))
    }
}

fn css_for<'a>(selectors: impl Iterator<Item = &'a str>) -> String {
    let joined: Vec<&str> = selectors.collect();
    if joined.is_empty() {
        String::new()
    } else {
        format!("{} {{ display: none !important; }}", joined.join(",\n"))
    }
}

/// Android WebView doesn't report a request's type, so infer the adblock type
/// from the Accept header and the URL's file extension.
pub fn guess_request_type(url: &str, accept: Option<&str>) -> &'static str {
    let accept = accept.unwrap_or("").to_ascii_lowercase();
    if accept.contains("text/css") {
        return "stylesheet";
    }
    if accept.starts_with("image/") {
        return "image";
    }
    if accept.contains("text/html") {
        return "subdocument";
    }
    let path = url.split(['?', '#']).next().unwrap_or(url).to_ascii_lowercase();
    let ext = path.rsplit('/').next().and_then(|f| f.rsplit_once('.')).map(|(_, e)| e).unwrap_or("");
    match ext {
        "js" | "mjs" => "script",
        "css" => "stylesheet",
        "png" | "jpg" | "jpeg" | "gif" | "webp" | "svg" | "ico" | "avif" => "image",
        "woff" | "woff2" | "ttf" | "otf" => "font",
        "mp4" | "webm" | "m3u8" | "mp3" | "ts" | "m4s" => "media",
        "html" | "htm" => "subdocument",
        _ => "other",
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    const RULES: &str = "\
||ads.example.com^
||tracker.example.net^$third-party
@@||ads.example.com/allowed.js
example.org##.ad-banner
##.sponsored-box
";

    fn blocker() -> Blocker {
        Blocker::from_lists(&[RULES])
    }

    #[test]
    fn blocks_ad_domain() {
        let b = blocker();
        assert_eq!(b.check("https://ads.example.com/banner.js", "https://news.example.org/", None, false), Decision::Block);
    }

    #[test]
    fn allows_normal_request() {
        let b = blocker();
        assert_eq!(b.check("https://cdn.example.org/app.js", "https://example.org/", None, false), Decision::Allow);
    }

    #[test]
    fn exception_rule_wins() {
        let b = blocker();
        assert_eq!(b.check("https://ads.example.com/allowed.js", "https://news.example.org/", None, false), Decision::Allow);
    }

    #[test]
    fn third_party_only_rule() {
        let b = blocker();
        assert_eq!(b.check("https://tracker.example.net/p.gif", "https://shop.example.org/", None, false), Decision::Block);
        assert_eq!(b.check("https://tracker.example.net/p.gif", "https://tracker.example.net/", None, false), Decision::Allow);
    }

    #[test]
    fn never_blocks_main_frame() {
        let b = blocker();
        assert_eq!(b.check("https://ads.example.com/", "", None, true), Decision::Allow);
    }

    #[test]
    fn site_specific_cosmetic() {
        let b = blocker();
        assert!(b.cosmetic("https://example.org/page").hide_css.contains(".ad-banner"));
        assert!(!b.cosmetic("https://other.example.com/").hide_css.contains(".ad-banner"));
    }

    #[test]
    fn generic_cosmetic() {
        let b = blocker();
        let css = b.generic_css(&["sponsored-box".into()], &[], &[]);
        assert!(css.contains(".sponsored-box"));
    }

    #[test]
    fn serialize_round_trip() {
        let bytes = blocker().serialize();
        let b = Blocker::from_serialized(&bytes).expect("loads");
        assert_eq!(b.check("https://ads.example.com/x.js", "https://news.example.org/", None, false), Decision::Block);
    }

    #[test]
    fn request_type_guessing() {
        assert_eq!(guess_request_type("https://a.com/x.js?v=1", None), "script");
        assert_eq!(guess_request_type("https://a.com/x", Some("text/css,*/*;q=0.1")), "stylesheet");
        assert_eq!(guess_request_type("https://a.com/x", Some("image/avif,image/webp")), "image");
        assert_eq!(guess_request_type("https://a.com/api", None), "other");
    }

    #[test]
    fn blocker_is_thread_safe() {
        fn assert_send_sync<T: Send + Sync>() {}
        assert_send_sync::<Blocker>();
    }
}
