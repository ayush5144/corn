// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

//! Exporting the Vault: pure formatting, no I/O (see docs/02-technical/13-vault-export.md).
//!
//! - Links → an HTML bookmarks file (the "Netscape bookmark file" every browser imports), with
//!   everything inside one "Corn Vault" folder and a folder per Collection.
//! - Links → CSV (`title,url,collection,tags,saved_at`), for spreadsheets and apps.
//! - Passwords → CSV in Chrome's format (`name,url,username,password,note`). Passwords are
//!   decrypted by the app (the key lives in the phone's keystore) and handed here as plain rows.
//!
//! CSV: UTF-8, `\r\n` line ends, RFC 4180 quoting. The links CSV guards against spreadsheet
//! formulas; the passwords CSV never does (it would change passwords and break imports).

use crate::VaultItem;

/// A Collection as the export needs it (name and when it was made).
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Folder {
    pub id: i64,
    pub name: String,
    /// Milliseconds since 1970.
    pub created_at: i64,
}

/// One saved login in clear, for the passwords CSV.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct PasswordRow {
    /// `https://host[:port]`.
    pub origin: String,
    pub username: String,
    pub password: String,
}

/// The top folder every exported link sits in, so an import doesn't mix with existing bookmarks.
pub const VAULT_FOLDER: &str = "Corn Vault";

// ---------------- HTML bookmarks ----------------

/// Every link as an HTML bookmarks file. `exported_at` (milliseconds) dates the "Corn Vault" folder.
/// Links keep the Vault's order (as given); Collections keep theirs, each as a folder, then the
/// links in no Collection.
pub fn links_html(folders: &[Folder], items: &[VaultItem], exported_at: i64) -> String {
    let mut out = String::from(
        "<!DOCTYPE NETSCAPE-Bookmark-file-1>\n\
         <!-- This is an automatically generated file.\n     It will be read and overwritten.\n     DO NOT EDIT! -->\n\
         <META HTTP-EQUIV=\"Content-Type\" CONTENT=\"text/html; charset=UTF-8\">\n\
         <TITLE>Bookmarks</TITLE>\n\
         <H1>Bookmarks</H1>\n\
         <DL><p>\n",
    );
    out.push_str(&format!("    <DT><H3 ADD_DATE=\"{}\">{}</H3>\n", seconds(exported_at), VAULT_FOLDER));
    out.push_str("    <DL><p>\n");
    for f in folders {
        out.push_str(&format!("        <DT><H3 ADD_DATE=\"{}\">{}</H3>\n", seconds(f.created_at), html_escape(&one_line(&f.name))));
        out.push_str("        <DL><p>\n");
        for i in items.iter().filter(|i| i.collection_id == Some(f.id)) {
            push_link(&mut out, i, "            ");
        }
        out.push_str("        </DL><p>\n");
    }
    for i in items.iter().filter(|i| !in_a_folder(i, folders)) {
        push_link(&mut out, i, "        ");
    }
    out.push_str("    </DL><p>\n");
    out.push_str("</DL><p>\n");
    out
}

fn in_a_folder(i: &VaultItem, folders: &[Folder]) -> bool {
    i.collection_id.is_some_and(|id| folders.iter().any(|f| f.id == id))
}

fn push_link(out: &mut String, i: &VaultItem, indent: &str) {
    let title = one_line(&i.title);
    let text = if title.trim().is_empty() { one_line(&i.url) } else { title };
    out.push_str(&format!("{indent}<DT><A HREF=\"{}\" ADD_DATE=\"{}\"", html_escape(&one_line(&i.url)), seconds(i.created_at)));
    if !i.tags.is_empty() {
        out.push_str(&format!(" TAGS=\"{}\"", html_escape(&one_line(&i.tags.join(",")))));
    }
    out.push_str(&format!(">{}</A>\n", html_escape(&text)));
}

/// `& < > "` escaped, so a title or address can't break the file.
pub fn html_escape(s: &str) -> String {
    let mut out = String::with_capacity(s.len());
    for c in s.chars() {
        match c {
            '&' => out.push_str("&amp;"),
            '<' => out.push_str("&lt;"),
            '>' => out.push_str("&gt;"),
            '"' => out.push_str("&quot;"),
            _ => out.push(c),
        }
    }
    out
}

/// Line breaks become spaces: a bookmark's title is one line.
fn one_line(s: &str) -> String {
    s.replace("\r\n", " ").replace(['\r', '\n'], " ")
}

fn seconds(ms: i64) -> i64 {
    ms.div_euclid(1000)
}

// ---------------- CSV ----------------

/// Every link as CSV: `title,url,collection,tags,saved_at`. Tags joined with `; `, `saved_at` in
/// UTC ISO 8601, collection empty for a link in no Collection. Formula-guarded.
pub fn links_csv(folders: &[Folder], items: &[VaultItem]) -> String {
    let mut out = String::from("title,url,collection,tags,saved_at\r\n");
    for i in items {
        let collection = i
            .collection_id
            .and_then(|id| folders.iter().find(|f| f.id == id))
            .map(|f| f.name.as_str())
            .unwrap_or("");
        let fields = [i.title.as_str(), i.url.as_str(), collection, &i.tags.join("; "), &iso_utc(i.created_at)];
        push_row(&mut out, fields.iter().map(|f| guard_formula(f)));
    }
    out
}

/// Saved passwords in Chrome's CSV format: `name,url,username,password,note`. `name` is the site's
/// host, `url` its origin, `note` empty. Sorted by site, then username. **No formula guard**: the
/// values must import exactly as saved.
pub fn passwords_csv(rows: &[PasswordRow]) -> String {
    let mut sorted: Vec<&PasswordRow> = rows.iter().collect();
    sorted.sort_by(|a, b| {
        host_of(&a.origin)
            .to_lowercase()
            .cmp(&host_of(&b.origin).to_lowercase())
            .then_with(|| a.origin.cmp(&b.origin))
            .then_with(|| a.username.cmp(&b.username))
    });
    let mut out = String::from("name,url,username,password,note\r\n");
    for r in sorted {
        let fields = [host_of(&r.origin), r.origin.as_str(), r.username.as_str(), r.password.as_str(), ""];
        push_row(&mut out, fields.iter().map(|f| f.to_string()));
    }
    out
}

/// `https://mail.example.com:8443` → `mail.example.com`.
pub fn host_of(origin: &str) -> &str {
    let rest = origin.split_once("://").map(|(_, r)| r).unwrap_or(origin);
    let end = rest.find(['/', ':', '?', '#']).unwrap_or(rest.len());
    &rest[..end]
}

fn push_row(out: &mut String, fields: impl Iterator<Item = String>) {
    let row: Vec<String> = fields.map(|f| csv_field(&f)).collect();
    out.push_str(&row.join(","));
    out.push_str("\r\n");
}

/// RFC 4180: quote a value holding a comma, quote or line break, or with space at either end;
/// double the quotes inside.
pub fn csv_field(s: &str) -> String {
    let needs_quotes = s.contains([',', '"', '\r', '\n'])
        || s.starts_with(char::is_whitespace)
        || s.ends_with(char::is_whitespace);
    if needs_quotes {
        format!("\"{}\"", s.replace('"', "\"\""))
    } else {
        s.to_owned()
    }
}

/// "CSV injection": a value starting with `= + - @`, a tab or a carriage return gets a leading `'`
/// so a spreadsheet shows it as text instead of running it. Links CSV only.
pub fn guard_formula(s: &str) -> String {
    if s.starts_with(['=', '+', '-', '@', '\t', '\r']) {
        format!("'{s}")
    } else {
        s.to_owned()
    }
}

// ---------------- dates ----------------

/// Milliseconds since 1970 → `2026-11-13T05:40:00Z` (UTC).
pub fn iso_utc(ms: i64) -> String {
    let secs = ms.div_euclid(1000);
    let days = secs.div_euclid(86_400);
    let rem = secs.rem_euclid(86_400);
    let (y, m, d) = civil_from_days(days);
    format!("{y:04}-{m:02}-{d:02}T{:02}:{:02}:{:02}Z", rem / 3600, rem % 3600 / 60, rem % 60)
}

/// Days since 1970-01-01 → (year, month, day), proleptic Gregorian (Howard Hinnant's algorithm).
fn civil_from_days(days: i64) -> (i64, u32, u32) {
    let z = days + 719_468;
    let era = z.div_euclid(146_097);
    let doe = z.rem_euclid(146_097);
    let yoe = (doe - doe / 1460 + doe / 36_524 - doe / 146_096) / 365;
    let doy = doe - (365 * yoe + yoe / 4 - yoe / 100);
    let mp = (5 * doy + 2) / 153;
    let d = (doy - (153 * mp + 2) / 5 + 1) as u32;
    let m = if mp < 10 { mp + 3 } else { mp - 9 } as u32;
    let y = yoe + era * 400 + i64::from(m <= 2);
    (y, m, d)
}

#[cfg(test)]
mod tests {
    use super::*;

    fn item(id: i64, collection_id: Option<i64>, title: &str, url: &str, tags: &[&str], created_at: i64) -> VaultItem {
        VaultItem {
            id,
            collection_id,
            title: title.into(),
            url: url.into(),
            tags: tags.iter().map(|t| t.to_string()).collect(),
            created_at,
        }
    }

    fn sample() -> (Vec<Folder>, Vec<VaultItem>) {
        let folders = vec![
            Folder { id: 1, name: "Recipes & more".into(), created_at: 1_700_000_000_123 },
            Folder { id: 2, name: "Empty one".into(), created_at: 1_700_000_100_000 },
        ];
        let items = vec![
            item(10, Some(1), "Quiet pasta, \"fast\" <b>", "https://a.example/p?x=1&y=2", &["pasta", "quick"], 1_763_012_400_999),
            item(11, None, "Line\nbreak 🌽", "https://b.example/", &[], 0),
            item(12, None, "=HYPERLINK(\"evil\")", "https://c.example/", &["-dash"], 86_400_000),
        ];
        (folders, items)
    }

    #[test]
    fn dates_in_utc() {
        assert_eq!(iso_utc(0), "1970-01-01T00:00:00Z");
        assert_eq!(iso_utc(1_763_012_400_999), "2025-11-13T05:40:00Z");
        assert_eq!(iso_utc(951_782_400_000), "2000-02-29T00:00:00Z"); // leap day
        assert_eq!(iso_utc(1_798_761_599_000), "2026-12-31T23:59:59Z");
        assert_eq!(iso_utc(-1000), "1969-12-31T23:59:59Z");
        assert_eq!(seconds(1_700_000_000_999), 1_700_000_000);
    }

    #[test]
    fn html_has_one_vault_folder_a_folder_per_collection_and_escapes() {
        let (folders, items) = sample();
        let html = links_html(&folders, &items, 1_763_000_000_000);
        assert!(html.starts_with("<!DOCTYPE NETSCAPE-Bookmark-file-1>\n"));
        assert!(html.contains("<META HTTP-EQUIV=\"Content-Type\" CONTENT=\"text/html; charset=UTF-8\">"));
        assert!(html.contains("<DT><H3 ADD_DATE=\"1763000000\">Corn Vault</H3>"));
        assert!(html.contains("<DT><H3 ADD_DATE=\"1700000000\">Recipes &amp; more</H3>"));
        assert!(html.contains("<DT><H3 ADD_DATE=\"1700000100\">Empty one</H3>")); // empty Collections kept
        assert!(html.contains(
            "            <DT><A HREF=\"https://a.example/p?x=1&amp;y=2\" ADD_DATE=\"1763012400\" TAGS=\"pasta,quick\">Quiet pasta, &quot;fast&quot; &lt;b&gt;</A>\n"
        ));
        // Not in a Collection: directly inside Corn Vault, no TAGS when there are none, one line.
        assert!(html.contains("        <DT><A HREF=\"https://b.example/\" ADD_DATE=\"0\">Line break 🌽</A>\n"));
        // Balanced lists: root, Corn Vault, two Collections.
        assert_eq!(html.matches("<DL><p>").count(), 4);
        assert_eq!(html.matches("</DL><p>").count(), 4);
        assert_eq!(html.matches("<DT><A ").count(), 3);
    }

    #[test]
    fn html_link_without_title_shows_its_address() {
        let html = links_html(&[], &[item(1, None, "  ", "https://x.example/", &[], 0)], 0);
        assert!(html.contains(">https://x.example/</A>"));
    }

    #[test]
    fn empty_vault_exports_valid_empty_files() {
        let html = links_html(&[], &[], 0);
        assert!(html.contains("Corn Vault"));
        assert_eq!(html.matches("<DT><A ").count(), 0);
        assert_eq!(links_csv(&[], &[]), "title,url,collection,tags,saved_at\r\n");
        assert_eq!(passwords_csv(&[]), "name,url,username,password,note\r\n");
    }

    #[test]
    fn links_csv_quotes_dates_tags_and_guards_formulas() {
        let (folders, items) = sample();
        let csv = links_csv(&folders, &items);
        let lines: Vec<&str> = csv.split("\r\n").collect();
        assert_eq!(lines[0], "title,url,collection,tags,saved_at");
        assert_eq!(
            lines[1],
            "\"Quiet pasta, \"\"fast\"\" <b>\",https://a.example/p?x=1&y=2,Recipes & more,pasta; quick,2025-11-13T05:40:00Z"
        );
        // A line break inside a value stays, inside quotes.
        assert_eq!(lines[2], "\"Line\nbreak 🌽\",https://b.example/,,,1970-01-01T00:00:00Z");
        // Formula guard on any column.
        assert_eq!(lines[3], "\"'=HYPERLINK(\"\"evil\"\")\",https://c.example/,,'-dash,1970-01-02T00:00:00Z");
        assert!(csv.ends_with("\r\n"));
        assert!(!csv.replace("\r\n", "").contains('\r'));
    }

    #[test]
    fn csv_field_rules() {
        assert_eq!(csv_field("plain"), "plain");
        assert_eq!(csv_field("a,b"), "\"a,b\"");
        assert_eq!(csv_field("say \"hi\""), "\"say \"\"hi\"\"\"");
        assert_eq!(csv_field(" padded"), "\" padded\"");
        assert_eq!(csv_field("padded "), "\"padded \"");
        assert_eq!(csv_field("a\r\nb"), "\"a\r\nb\"");
        assert_eq!(csv_field(""), "");
        for (i, o) in [("=1+1", "'=1+1"), ("+1", "'+1"), ("-1", "'-1"), ("@x", "'@x"), ("\tx", "'\tx"), ("\rx", "'\rx"), ("a=b", "a=b")] {
            assert_eq!(guard_formula(i), o);
        }
    }

    #[test]
    fn passwords_csv_is_chrome_format_without_formula_guard() {
        let rows = vec![
            PasswordRow { origin: "https://zeta.example".into(), username: "z@example.com".into(), password: "=SUM(A1)".into() },
            PasswordRow { origin: "https://mail.example.com:8443".into(), username: "me, myself".into(), password: "-p\"w,d ".into() },
            PasswordRow { origin: "https://mail.example.com:8443".into(), username: "".into(), password: "🌽secret".into() },
        ];
        let csv = passwords_csv(&rows);
        assert_eq!(
            csv,
            "name,url,username,password,note\r\n\
             mail.example.com,https://mail.example.com:8443,,🌽secret,\r\n\
             mail.example.com,https://mail.example.com:8443,\"me, myself\",\"-p\"\"w,d \",\r\n\
             zeta.example,https://zeta.example,z@example.com,=SUM(A1),\r\n"
        );
    }

    #[test]
    fn host_of_origins() {
        assert_eq!(host_of("https://example.com"), "example.com");
        assert_eq!(host_of("https://m.example.com:8443"), "m.example.com");
        assert_eq!(host_of("http://10.0.2.2:8000/"), "10.0.2.2");
        assert_eq!(host_of("example.com"), "example.com");
    }
}
