// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

//! Scriptlet resources for `+js(...)` rules, assembled from uBlock Origin's own source files.
//!
//! adblock-rust needs the scriptlet code as [`Resource`]s. Brave builds them on its servers from
//! uBO's `src/js/resources/*.js` modules (each registered scriptlet's `fn.toString()`) and ships
//! the result in a component only Brave's updater can fetch. Flush does the same on the phone:
//! the app downloads uBO's unmodified module files and this code turns them into resources.
//!
//! uBO's modules are ES modules. Running them would need a JavaScript engine, so this reads them
//! as text instead, relying on uBO's long-standing layout: top-level declarations start at
//! column 0 (`function x(`, `async function x(`, `class X {`, optionally `export`ed) and end at
//! the next line that is exactly `}`; registrations are `registerScriptlet(fn, { … });` or
//! `builtinScriptlets.push({ … });`. That text is exactly what `fn.toString()` returns. If uBO
//! ever changes layout, [`assemble`] fails its checks and the app keeps its last good copy.

use std::collections::{HashMap, HashSet};
use std::sync::LazyLock;

use adblock::resources::{MimeType, PermissionMask, Resource, ResourceType};
use base64::{engine::general_purpose::STANDARD as BASE64, Engine as _};
use regex::Regex;

/// Permission bit for uBO's "trusted" scriptlets (`requiresTrust: true`, e.g.
/// `trusted-replace-xhr-response`). Only lists Flush marks as trusted can use them, the same bit
/// Brave uses for its default lists.
pub const TRUSTED: PermissionMask = PermissionMask::from_bits(1);

/// Scriptlets every usable set must contain (YouTube's rules rely on them).
const REQUIRED: &[&str] = &["safe-self.fn", "set-constant.js", "json-prune.js", "trusted-replace-xhr-response.js"];
/// A real uBO release registers well over 100 scriptlets and helpers.
const MIN_RESOURCES: usize = 80;

#[derive(Debug, thiserror::Error, PartialEq, Eq)]
pub enum ScriptletError {
    #[error("only {0} scriptlets found")]
    TooFew(usize),
    #[error("scriptlet {0} is missing")]
    Missing(String),
    #[error("the code for scriptlet {0} was not found")]
    NoCode(String),
    #[error("scriptlet {0} depends on {1}, which is unknown")]
    UnknownDependency(String, String),
    #[error("stored scriptlets are unreadable: {0}")]
    Json(String),
}

static IMPORT_RE: LazyLock<Regex> =
    LazyLock::new(|| Regex::new(r#"(?m)^\s*(?:import|export)\b[^;]*?\bfrom\s*['"]([^'"]+)['"]|^\s*import\s*['"]([^'"]+)['"]"#).unwrap());
static DECL_RE: LazyLock<Regex> = LazyLock::new(|| {
    Regex::new(r"^(?:export\s+)?(?:default\s+)?((?:async\s+)?function\s*\*?\s*([A-Za-z_$][\w$]*)\s*\(|class\s+([A-Za-z_$][\w$]*)\b)").unwrap()
});
static REGISTER_RE: LazyLock<Regex> =
    LazyLock::new(|| Regex::new(r"^registerScriptlet\(\s*([A-Za-z_$][\w$]*)\s*,\s*\{").unwrap());
static PUSH_RE: LazyLock<Regex> = LazyLock::new(|| Regex::new(r"^builtinScriptlets\.push\(\s*\{").unwrap());
static ALIAS_CONST_RE: LazyLock<Regex> =
    LazyLock::new(|| Regex::new(r"(?m)^(?:export\s+)?const\s+([A-Za-z_$][\w$]*)\s*=\s*([A-Za-z_$][\w$]*)\s*;").unwrap());
static IMPORT_AS_RE: LazyLock<Regex> =
    LazyLock::new(|| Regex::new(r"\b([A-Za-z_$][\w$]*)\s+as\s+([A-Za-z_$][\w$]*)\b").unwrap());
static IMPORT_BLOCK_RE: LazyLock<Regex> = LazyLock::new(|| Regex::new(r"(?ms)^import\s*\{([^}]*)\}").unwrap());
static NAME_RE: LazyLock<Regex> = LazyLock::new(|| Regex::new(r#"\bname:\s*['"]([^'"]+)['"]"#).unwrap());
static FN_RE: LazyLock<Regex> = LazyLock::new(|| Regex::new(r"\bfn:\s*([A-Za-z_$][\w$]*)").unwrap());
static ALIASES_RE: LazyLock<Regex> = LazyLock::new(|| Regex::new(r"(?s)\baliases:\s*\[([^\]]*)\]").unwrap());
static DEPS_RE: LazyLock<Regex> = LazyLock::new(|| Regex::new(r"(?s)\bdependencies:\s*\[([^\]]*)\]").unwrap());
static TRUST_RE: LazyLock<Regex> = LazyLock::new(|| Regex::new(r"\brequiresTrust:\s*true\b").unwrap());
static QUOTED_RE: LazyLock<Regex> = LazyLock::new(|| Regex::new(r#"['"]([^'"]+)['"]"#).unwrap());
static IDENT_RE: LazyLock<Regex> = LazyLock::new(|| Regex::new(r"^[A-Za-z_$][\w$]*$").unwrap());

/// The relative module paths one uBO module imports (`./x.js`, `../y.js`), in order, without
/// duplicates. The app uses this to download the whole module graph starting at `scriptlets.js`.
pub fn module_imports(source: &str) -> Vec<String> {
    let mut seen = HashSet::new();
    IMPORT_RE
        .captures_iter(source)
        .filter_map(|c| c.get(1).or_else(|| c.get(2)).map(|m| m.as_str().to_owned()))
        .filter(|p| (p.starts_with("./") || p.starts_with("../")) && p.ends_with(".js"))
        .filter(|p| seen.insert(p.clone()))
        .collect()
}

struct Registration {
    name: String,
    fn_ident: String,
    aliases: Vec<String>,
    /// Either scriptlet names (quoted) or function identifiers, resolved later.
    deps: Vec<Dep>,
    trusted: bool,
}

enum Dep {
    Name(String),
    Ident(String),
}

/// Turn the text of uBO's scriptlet modules (all of them, any order) into adblock-rust resources.
pub fn assemble<S: AsRef<str>>(sources: &[S]) -> Result<Vec<Resource>, ScriptletError> {
    let mut code: HashMap<String, String> = HashMap::new();
    let mut aliases: HashMap<String, String> = HashMap::new();
    let mut regs: Vec<Registration> = Vec::new();

    for src in sources {
        let src = src.as_ref();
        read_declarations(src, &mut code);
        read_registrations(src, &mut regs);
        // `import { X as Y }` and `export const Z = Y;` make Z another name for X.
        for block in IMPORT_BLOCK_RE.captures_iter(src) {
            for c in IMPORT_AS_RE.captures_iter(&block[1]) {
                aliases.insert(c[2].to_owned(), c[1].to_owned());
            }
        }
        for c in ALIAS_CONST_RE.captures_iter(src) {
            aliases.insert(c[1].to_owned(), c[2].to_owned());
        }
    }

    let resolve = |ident: &str| -> Option<&String> {
        let mut cur = ident;
        for _ in 0..8 {
            if let Some(body) = code.get(cur) {
                return Some(body);
            }
            cur = aliases.get(cur)?;
        }
        None
    };
    // Function identifier → registered scriptlet name, for `dependencies: [ someFn ]`.
    let ident_to_name: HashMap<&str, &str> = regs.iter().map(|r| (r.fn_ident.as_str(), r.name.as_str())).collect();
    let names: HashSet<&str> = regs.iter().map(|r| r.name.as_str()).collect();

    let mut out = Vec::with_capacity(regs.len());
    let mut seen = HashSet::new();
    for r in &regs {
        if !seen.insert(r.name.as_str()) {
            continue; // registered twice: keep the first, as uBO's own lookup does
        }
        let body = resolve(&r.fn_ident).ok_or_else(|| ScriptletError::NoCode(r.name.clone()))?;
        let mut deps = Vec::with_capacity(r.deps.len());
        for d in &r.deps {
            let name = match d {
                Dep::Name(n) => n.as_str(),
                Dep::Ident(i) => ident_to_name.get(i.as_str()).copied().unwrap_or(i.as_str()),
            };
            if !names.contains(name) {
                return Err(ScriptletError::UnknownDependency(r.name.clone(), name.to_owned()));
            }
            deps.push(name.to_owned());
        }
        out.push(Resource {
            name: r.name.clone(),
            aliases: r.aliases.clone(),
            kind: ResourceType::Mime(MimeType::ApplicationJavascript),
            content: BASE64.encode(body),
            dependencies: deps,
            permission: if r.trusted { TRUSTED } else { PermissionMask::default() },
        });
    }

    if out.len() < MIN_RESOURCES {
        return Err(ScriptletError::TooFew(out.len()));
    }
    for want in REQUIRED {
        if !out.iter().any(|r| r.name == *want) {
            return Err(ScriptletError::Missing((*want).to_owned()));
        }
    }
    Ok(out)
}

/// Top-level declarations: from `function x(` / `class X` at column 0 to the next line that is `}`.
fn read_declarations(src: &str, code: &mut HashMap<String, String>) {
    let mut lines = src.lines();
    while let Some(line) = lines.next() {
        let Some(c) = DECL_RE.captures(line) else { continue };
        let name = c.get(2).or_else(|| c.get(3)).unwrap().as_str().to_owned();
        let start = c.get(1).unwrap().start();
        let mut body = String::from(&line[start..]);
        let mut closed = line.trim_end().ends_with('}') && line.contains('{') && one_line_balanced(line);
        while !closed {
            let Some(next) = lines.next() else { break };
            body.push('\n');
            body.push_str(next);
            closed = next.trim_end() == "}";
        }
        if closed {
            code.entry(name).or_insert(body);
        }
    }
}

fn one_line_balanced(line: &str) -> bool {
    line.matches('{').count() == line.matches('}').count()
}

fn read_registrations(src: &str, regs: &mut Vec<Registration>) {
    let mut lines = src.lines();
    while let Some(line) = lines.next() {
        let fn_ident = if let Some(c) = REGISTER_RE.captures(line) {
            Some(c[1].to_owned())
        } else if PUSH_RE.is_match(line) {
            None
        } else {
            continue;
        };
        let mut obj = String::from(line);
        if !line.trim_end().ends_with("});") {
            for next in lines.by_ref() {
                obj.push('\n');
                obj.push_str(next);
                if next.trim() == "});" {
                    break;
                }
            }
        }
        let Some(name) = NAME_RE.captures(&obj).map(|c| c[1].to_owned()) else { continue };
        let Some(fn_ident) = fn_ident.or_else(|| FN_RE.captures(&obj).map(|c| c[1].to_owned())) else { continue };
        let aliases = ALIASES_RE
            .captures(&obj)
            .map(|c| QUOTED_RE.captures_iter(&c[1]).map(|q| q[1].to_owned()).collect())
            .unwrap_or_default();
        let deps = DEPS_RE
            .captures(&obj)
            .map(|c| {
                c[1].split(',')
                    .map(str::trim)
                    .filter(|s| !s.is_empty())
                    .filter_map(|s| {
                        if let Some(q) = QUOTED_RE.captures(s) {
                            Some(Dep::Name(q[1].to_owned()))
                        } else if IDENT_RE.is_match(s) {
                            Some(Dep::Ident(s.to_owned()))
                        } else {
                            None
                        }
                    })
                    .collect()
            })
            .unwrap_or_default();
        regs.push(Registration { name, fn_ident, aliases, deps, trusted: TRUST_RE.is_match(&obj) });
    }
}

/// Resources in adblock-rust's JSON format (the same shape as Brave's `resources.json`).
pub fn to_json(resources: &[Resource]) -> String {
    serde_json::to_string(resources).expect("resources serialize")
}

pub fn from_json(json: &str) -> Result<Vec<Resource>, ScriptletError> {
    serde_json::from_str(json).map_err(|e| ScriptletError::Json(e.to_string()))
}

/// Wrap the engine's `injected_script` for a page: a private scope (so scriptlet helpers don't
/// become page globals) and the `scriptletGlobals` object uBO's scriptlets expect from their
/// injector. Empty in, empty out.
pub fn wrap_for_page(injected_script: &str) -> String {
    if injected_script.is_empty() {
        return String::new();
    }
    format!("(function () {{\nvar scriptletGlobals = {{}};\n{injected_script}\n}})();")
}

#[cfg(test)]
mod tests {
    use super::*;

    // Shaped like uBO's modules: registerScriptlet with function deps, builtinScriptlets.push
    // with string deps, an aliased class import, and a trusted scriptlet.
    const BASE: &str = r#"
export const registeredScriptlets = [];
export const registerScriptlet = (fn, details) => { registeredScriptlets.push(details); };
"#;
    const SAFE: &str = r#"
import { registerScriptlet } from './base.js';
export function safeSelf() {
    if ( safeSelf.safe ) { return safeSelf.safe; }
    safeSelf.safe = { log: function() {} };
    return safeSelf.safe;
}
registerScriptlet(safeSelf, {
    name: 'safe-self.fn',
});
"#;
    const SHARED: &str = r#"
import { JSONPath as __JSONPath__ } from '../jsonpath.js';
import { registerScriptlet } from './base.js';
export const JSONPath = __JSONPath__;
registerScriptlet(JSONPath, {
    name: 'jsonpath.fn',
});
"#;
    const JSONPATH: &str = r#"
export class JSONPath {
    get value() { return 1; }
}
"#;
    const SCRIPTLETS: &str = r#"
import './shared.js';
import {
    safeSelf,
} from './safe-self.js';
import { registeredScriptlets } from './base.js';
export const builtinScriptlets = registeredScriptlets;

builtinScriptlets.push({
    name: 'set-constant.js',
    aliases: [
        'set.js',
    ],
    fn: setConstant,
    dependencies: [
        'safe-self.fn',
    ],
});
function setConstant(chain = '', value = '') {
    const safe = safeSelf();
    if ( chain === '' ) { return; }
    window.__setConstant = chain + '=' + value;
}

builtinScriptlets.push({
    name: 'json-prune.js',
    fn: jsonPrune,
    dependencies: [ 'jsonpath.fn', 'safe-self.fn' ],
});
function jsonPrune(paths = '') {
    window.__jsonPrune = paths;
}

builtinScriptlets.push({
    name: 'trusted-replace-xhr-response.js',
    requiresTrust: true,
    fn: trustedReplaceXhrResponse,
    dependencies: [
        'safe-self.fn',
    ],
});
async function trustedReplaceXhrResponse(pattern = '') {
    window.__trusted = pattern;
}
"#;

    fn sources() -> Vec<&'static str> {
        vec![BASE, SAFE, SHARED, JSONPATH, SCRIPTLETS]
    }

    fn assemble_unchecked(src: &[&str]) -> Vec<Resource> {
        // The real minimum is for real uBO releases; pad with dummies for the unit tests.
        let mut all: Vec<String> = src.iter().map(|s| s.to_string()).collect();
        let mut pad = String::new();
        for i in 0..MIN_RESOURCES {
            pad += &format!("function pad{i}() {{\n}}\nregisterScriptlet(pad{i}, {{\n    name: 'pad{i}.fn',\n}});\n");
        }
        all.push(pad);
        assemble(&all).expect("assembles")
    }

    fn get<'a>(rs: &'a [Resource], name: &str) -> &'a Resource {
        rs.iter().find(|r| r.name == name).unwrap_or_else(|| panic!("{name} missing"))
    }

    fn text(r: &Resource) -> String {
        String::from_utf8(BASE64.decode(&r.content).unwrap()).unwrap()
    }

    #[test]
    fn imports_are_listed_once() {
        let imports = module_imports(SCRIPTLETS);
        assert_eq!(imports, vec!["./shared.js", "./safe-self.js", "./base.js"]);
        assert_eq!(module_imports(SHARED), vec!["../jsonpath.js", "./base.js"]);
        assert!(module_imports("import x from 'https://evil.example/x.js';").is_empty());
    }

    #[test]
    fn assembles_function_text_like_fn_to_string() {
        let rs = assemble_unchecked(&sources());
        let set = get(&rs, "set-constant.js");
        assert_eq!(set.aliases, vec!["set.js"]);
        assert_eq!(set.dependencies, vec!["safe-self.fn"]);
        let body = text(set);
        assert!(body.starts_with("function setConstant(chain = '', value = '') {"));
        assert!(body.ends_with("\n}"));
        assert!(text(get(&rs, "safe-self.fn")).starts_with("function safeSelf()"), "export keyword dropped");
        assert!(text(get(&rs, "trusted-replace-xhr-response.js")).starts_with("async function trustedReplaceXhrResponse("));
    }

    #[test]
    fn aliased_class_imports_resolve() {
        let rs = assemble_unchecked(&sources());
        assert!(text(get(&rs, "jsonpath.fn")).starts_with("class JSONPath {"));
    }

    #[test]
    fn trusted_scriptlets_need_permission() {
        let rs = assemble_unchecked(&sources());
        assert_eq!(get(&rs, "trusted-replace-xhr-response.js").permission.to_bits(), TRUSTED.to_bits());
        assert_eq!(get(&rs, "set-constant.js").permission.to_bits(), 0);
    }

    #[test]
    fn rejects_incomplete_sets() {
        assert_eq!(assemble(&sources()).err(), Some(ScriptletError::TooFew(5)));
        let broken = SCRIPTLETS.replace("function jsonPrune(", "const jsonPrune = function(");
        let mut src = sources();
        src[4] = &broken;
        let mut all: Vec<String> = src.iter().map(|s| s.to_string()).collect();
        all.push((0..MIN_RESOURCES).map(|i| format!("function p{i}() {{\n}}\nregisterScriptlet(p{i}, {{\n    name: 'p{i}.fn',\n}});\n")).collect());
        assert_eq!(assemble(&all).err(), Some(ScriptletError::NoCode("json-prune.js".into())));
    }

    #[test]
    fn json_round_trip() {
        let rs = assemble_unchecked(&sources());
        let back = from_json(&to_json(&rs)).unwrap();
        assert_eq!(back.len(), rs.len());
        assert!(from_json("not json").is_err());
    }

    #[test]
    fn wrap_is_empty_for_nothing() {
        assert_eq!(wrap_for_page(""), "");
        assert!(wrap_for_page("x()").contains("var scriptletGlobals = {};\nx()"));
    }
}
