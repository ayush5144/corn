// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

//! Measure scriptlets on real data: assemble uBO's scriptlet modules, build the ads engine the
//! way the app does, and time the per-page scriptlet lookup.
//!
//! cargo run --release -p flush-block --example scriptlets -- <ubo-js-dir> <out-dir> <easylist.txt> <ubo-list.txt>...
//!
//! `<ubo-js-dir>` holds uBO's `src/js` files (at least `resources/*.js` and what they import).
//! uBO lists (all files after EasyList) are loaded as trusted, with their `!#include`s resolved
//! from the same folder. Writes the script for each test page to `<out-dir>/<host>.js`.

use std::path::{Path, PathBuf};
use std::time::Instant;

use flush_block::{preprocess, scriptlets, Blocker, FilterList};

fn js_files(dir: &Path, out: &mut Vec<PathBuf>) {
    for e in std::fs::read_dir(dir).expect("read dir").flatten() {
        let p = e.path();
        if p.is_dir() {
            js_files(&p, out);
        } else if p.extension().is_some_and(|x| x == "js") {
            out.push(p);
        }
    }
}

fn main() {
    let args: Vec<String> = std::env::args().skip(1).collect();
    assert!(args.len() >= 4, "usage: scriptlets <ubo-js-dir> <out-dir> <easylist.txt> <ubo-list.txt>...");
    let (js_dir, out_dir) = (Path::new(&args[0]), Path::new(&args[1]));

    let mut files = Vec::new();
    js_files(js_dir, &mut files);
    let sources: Vec<String> = files.iter().map(|p| std::fs::read_to_string(p).unwrap()).collect();
    let t = Instant::now();
    let resources = scriptlets::assemble(&sources).expect("assemble");
    let assemble_ms = t.elapsed().as_secs_f64() * 1e3;
    let json = scriptlets::to_json(&resources);
    let trusted = resources.iter().filter(|r| r.permission.to_bits() != 0).count();
    println!(
        "scriptlets: {} from {} files ({} trusted) in {:.1} ms; stored JSON {:.0} KB",
        resources.len(),
        files.len(),
        trusted,
        assemble_ms,
        json.len() as f64 / 1024.0
    );
    let t = Instant::now();
    let reloaded = scriptlets::from_json(&json).unwrap();
    println!("parse stored JSON: {:.1} ms", t.elapsed().as_secs_f64() * 1e3);

    let mut lists: Vec<FilterList<String>> = vec![FilterList { text: std::fs::read_to_string(&args[2]).unwrap(), trusted: false }];
    for path in &args[3..] {
        let text = std::fs::read_to_string(path).unwrap();
        let dir = Path::new(path).parent().unwrap();
        for inc in preprocess::preprocess(&text).includes {
            let inc_text = std::fs::read_to_string(dir.join(&inc)).unwrap_or_else(|_| panic!("include {inc} missing"));
            lists.push(FilterList { text: inc_text, trusted: true });
        }
        lists.push(FilterList { text, trusted: true });
    }
    let lines: usize = lists.iter().map(|l| l.text.lines().count()).sum();

    let t = Instant::now();
    let built = Blocker::from_filter_lists(&lists, reloaded.clone());
    let build_ms = t.elapsed().as_secs_f64() * 1e3;
    let bytes = built.serialize();
    let t = Instant::now();
    let b = Blocker::from_serialized(&bytes, reloaded).unwrap();
    let load_ms = t.elapsed().as_secs_f64() * 1e3;
    println!(
        "ads engine: {} lists, {} lines, build {:.0} ms, saved {:.1} MB, load+resources {:.1} ms",
        lists.len(),
        lines,
        build_ms,
        bytes.len() as f64 / 1_048_576.0,
        load_ms
    );

    std::fs::create_dir_all(out_dir).unwrap();
    for url in [
        "https://m.youtube.com/watch?v=kJQP7kiw5Fk",
        "https://www.youtube.com/watch?v=kJQP7kiw5Fk",
        "https://www.youtube-nocookie.com/embed/kJQP7kiw5Fk",
        "https://en.wikipedia.org/wiki/Corn",
        "https://example.com/",
    ] {
        let host = url.split('/').nth(2).unwrap();
        let s = b.scriptlets(url);
        let n = 200;
        let t = Instant::now();
        for _ in 0..n {
            std::hint::black_box(b.scriptlets(url));
        }
        let us = t.elapsed().as_secs_f64() * 1e6 / n as f64;
        let calls = s.matches("try {\n").count();
        println!("{host}: {} scriptlet calls, {:.1} KB, lookup {:.0} µs", calls, s.len() as f64 / 1024.0, us);
        if !s.is_empty() {
            std::fs::write(out_dir.join(format!("{host}.js")), &s).unwrap();
        }
    }
}
