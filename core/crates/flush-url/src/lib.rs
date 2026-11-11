// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

//! flush-url: URL hygiene shared by every Flush platform.
//!
//! - [`clean`] removes well-known tracking parameters (`utm_*`, `fbclid`, `gclid`…)
//!   from a link before it's opened. The page is the same; the site just can't see
//!   where you came from.
//! - [`https_upgrade`] turns an `http://` address into `https://` (the app falls back
//!   to http if the secure version fails to load).

use url::Url;

/// Exact parameter names that only exist to track clicks. Kept deliberately conservative:
/// removing a parameter a site actually needs would break the page.
const TRACKING_PARAMS: &[&str] = &[
    // Ad-click IDs
    "fbclid", "gclid", "gclsrc", "dclid", "gbraid", "wbraid", "msclkid", "yclid", "twclid", "ttclid",
    "li_fat_id", "epik", "rdt_cid", "irclickid", "s_cid",
    // Email / marketing platforms
    "mc_cid", "mc_eid", "_hsenc", "_hsmi", "hsCtaTracking", "mkt_tok", "vero_id", "vero_conv",
    "oly_anon_id", "oly_enc_id", "rb_clickid", "wickedid", "_openstat",
    // Social share tags
    "igshid", "igsh",
];

/// Parameter name prefixes used only for campaign tracking.
const TRACKING_PREFIXES: &[&str] = &["utm_", "__hs", "pk_", "mtm_"];

fn is_tracking(name: &str) -> bool {
    TRACKING_PARAMS.iter().any(|p| p.eq_ignore_ascii_case(name))
        || TRACKING_PREFIXES.iter().any(|p| name.len() > p.len() && name[..p.len()].eq_ignore_ascii_case(p))
}

/// Remove tracking parameters. Returns the input unchanged if there's nothing to remove
/// or it isn't an http(s) URL.
pub fn clean(input: &str) -> String {
    let Ok(mut url) = Url::parse(input) else { return input.to_owned() };
    if !matches!(url.scheme(), "http" | "https") || url.query().is_none() {
        return input.to_owned();
    }
    let pairs: Vec<(String, String)> = url.query_pairs().map(|(k, v)| (k.into_owned(), v.into_owned())).collect();
    let kept: Vec<&(String, String)> = pairs.iter().filter(|(k, _)| !is_tracking(k)).collect();
    if kept.len() == pairs.len() {
        return input.to_owned();
    }
    if kept.is_empty() {
        url.set_query(None);
    } else {
        url.query_pairs_mut().clear().extend_pairs(kept.iter().map(|(k, v)| (k.as_str(), v.as_str())));
    }
    url.into()
}

/// The https version of an http URL, or None if it's already secure, not http,
/// or points at this device / a local network (where https usually doesn't exist).
pub fn https_upgrade(input: &str) -> Option<String> {
    let mut url = Url::parse(input).ok()?;
    if url.scheme() != "http" {
        return None;
    }
    if is_local(&url) {
        return None;
    }
    url.set_scheme("https").ok()?;
    if url.port() == Some(80) {
        url.set_port(None).ok()?;
    }
    Some(url.into())
}

fn is_local(url: &Url) -> bool {
    match url.host() {
        Some(url::Host::Domain(d)) => {
            let d = d.to_ascii_lowercase();
            d == "localhost" || d.ends_with(".local") || d.ends_with(".localhost") || !d.contains('.')
        }
        Some(url::Host::Ipv4(ip)) => ip.is_private() || ip.is_loopback() || ip.is_link_local(),
        Some(url::Host::Ipv6(ip)) => ip.is_loopback() || (ip.segments()[0] & 0xfe00) == 0xfc00,
        None => true,
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn removes_tracking_keeps_real_params() {
        assert_eq!(
            clean("https://shop.example.com/item?id=42&utm_source=fb&utm_campaign=x&fbclid=AbC"),
            "https://shop.example.com/item?id=42"
        );
    }

    #[test]
    fn removes_query_entirely_when_only_tracking() {
        assert_eq!(clean("https://example.com/a?gclid=1&utm_medium=cpc"), "https://example.com/a");
    }

    #[test]
    fn leaves_clean_urls_untouched() {
        let u = "https://example.com/search?q=flush&page=2#top";
        assert_eq!(clean(u), u);
        assert_eq!(clean("not a url"), "not a url");
        assert_eq!(clean("mailto:a@example.com?subject=hi"), "mailto:a@example.com?subject=hi");
    }

    #[test]
    fn keeps_fragment() {
        assert_eq!(clean("https://example.com/p?utm_source=x#section"), "https://example.com/p#section");
    }

    #[test]
    fn does_not_strip_lookalike_names() {
        // "utm" alone and "pk" alone aren't tracking prefixes.
        let u = "https://example.com/?utm=1&pk=2&page_id=3";
        assert_eq!(clean(u), u);
    }

    #[test]
    fn upgrades_http() {
        assert_eq!(https_upgrade("http://example.com/a?b=1").as_deref(), Some("https://example.com/a?b=1"));
        assert_eq!(https_upgrade("http://example.com:80/").as_deref(), Some("https://example.com/"));
    }

    #[test]
    fn skips_https_and_local() {
        assert_eq!(https_upgrade("https://example.com/"), None);
        assert_eq!(https_upgrade("http://localhost:8080/"), None);
        assert_eq!(https_upgrade("http://10.0.2.2:8765/"), None);
        assert_eq!(https_upgrade("http://192.168.1.1/"), None);
        assert_eq!(https_upgrade("http://router/"), None);
        assert_eq!(https_upgrade("http://printer.local/"), None);
    }
}
