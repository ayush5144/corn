// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

//! Filter-list preprocessor directives (`!#if`, `!#else`, `!#endif`, `!#include`).
//!
//! uBlock Origin's lists use them to pick rules per browser and to split one list into several
//! files. adblock-rust treats them as comments, so without this step it would load both sides of
//! every `!#if`. Flush evaluates them as an Android, Chromium-based mobile browser, the same way
//! Brave's list packager does for Brave (plus `env_mobile`). Unknown conditions are false, as in
//! uBO.

/// Conditions Flush answers "true" to. Everything else is false.
const TRUE_CONDITIONS: &[&str] = &[
    "ext_ublock",          // the list's uBO-syntax rules are for us (Brave says the same)
    "env_chromium",        // Android System WebView is Chromium
    "env_mobile",          // a phone browser
    "cap_user_stylesheet", // matches Brave; Flush's hiding CSS is !important
    "adguard_ext_chromium",
];

fn condition(expr: &str) -> bool {
    let expr = expr.trim();
    if let Some(rest) = expr.strip_prefix('!') {
        return !condition(rest);
    }
    TRUE_CONDITIONS.contains(&expr)
}

/// A list with its `!#if` blocks resolved, plus the files it asks to include (in the active
/// branches only).
#[derive(Debug, Default, PartialEq, Eq)]
pub struct Preprocessed {
    pub text: String,
    pub includes: Vec<String>,
}

/// Resolve directives. Unbalanced `!#else` / `!#endif` lines are ignored rather than fatal, and
/// an unclosed `!#if` ends at the end of the list.
pub fn preprocess(list: &str) -> Preprocessed {
    // Each open `!#if` pushes whether its lines are kept (already false if a parent is false).
    let mut stack: Vec<(bool, bool)> = Vec::new(); // (this branch active, parent active)
    let mut out = String::with_capacity(list.len());
    let mut includes = Vec::new();
    for line in list.lines() {
        let t = line.trim();
        let active = stack.last().map_or(true, |(a, _)| *a);
        if let Some(expr) = t.strip_prefix("!#if ") {
            stack.push((active && condition(expr), active));
            continue;
        }
        if t == "!#else" {
            if let Some((a, parent)) = stack.pop() {
                stack.push((parent && !a, parent));
            }
            continue;
        }
        if t == "!#endif" {
            stack.pop();
            continue;
        }
        if !active {
            continue;
        }
        if let Some(name) = t.strip_prefix("!#include ") {
            let name = name.trim();
            if is_safe_include(name) {
                includes.push(name.to_owned());
            }
            continue;
        }
        out.push_str(line);
        out.push('\n');
    }
    Preprocessed { text: out, includes }
}

/// Includes are plain file names next to the list (uBO allows nothing else).
fn is_safe_include(name: &str) -> bool {
    !name.is_empty()
        && name.len() <= 100
        && name.ends_with(".txt")
        && name.chars().all(|c| c.is_ascii_alphanumeric() || matches!(c, '-' | '_' | '.'))
        && !name.contains("..")
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn keeps_the_true_branch() {
        let list = "a\n!#if env_firefox\nfirefox\n!#else\nnot-firefox\n!#endif\n!#if !cap_html_filtering\nno-html\n!#endif\nz\n";
        assert_eq!(preprocess(list).text, "a\nnot-firefox\nno-html\nz\n");
    }

    #[test]
    fn nested_false_parent_wins() {
        let list = "!#if env_safari\n!#if env_mobile\nx\n!#else\ny\n!#endif\n!#endif\nz\n";
        assert_eq!(preprocess(list).text, "z\n");
    }

    #[test]
    fn unknown_conditions_are_false() {
        assert_eq!(preprocess("!#if ext_ubol\nx\n!#endif\n").text, "");
    }

    #[test]
    fn includes_from_active_branches_only() {
        let list = "!#include filters-2026.txt\n!#if env_mobile\n!#include filters-mobile.txt\n!#endif\n!#if env_safari\n!#include safari.txt\n!#endif\n!#include ../../etc/passwd\n!#include https://evil.example/x.txt\n";
        assert_eq!(preprocess(list).includes, vec!["filters-2026.txt", "filters-mobile.txt"]);
    }

    #[test]
    fn stray_directives_are_harmless() {
        assert_eq!(preprocess("!#endif\na\n!#else\nb\n").text, "a\nb\n");
    }
}
