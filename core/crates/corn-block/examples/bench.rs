// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

//! Measure the engine on real filter lists.
//! Usage: cargo run --release --example bench -- easylist.txt easyprivacy.txt

use std::time::Instant;

use corn_block::{Blocker, Decision};

fn main() {
    let paths: Vec<String> = std::env::args().skip(1).collect();
    assert!(!paths.is_empty(), "pass one or more filter-list files");
    let lists: Vec<String> = paths.iter().map(|p| std::fs::read_to_string(p).expect("read list")).collect();
    let rules: usize = lists.iter().map(|l| l.lines().count()).sum();

    let t = Instant::now();
    let blocker = Blocker::from_lists(&lists);
    let build = t.elapsed();

    let bytes = blocker.serialize();
    let t = Instant::now();
    let cached = Blocker::from_serialized(&bytes).expect("load");
    let load = t.elapsed();

    let urls = [
        ("https://www.googletagmanager.com/gtm.js?id=GTM-XXXX", "https://news.example.com/"),
        ("https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js", "https://news.example.com/"),
        ("https://connect.facebook.net/en_US/fbevents.js", "https://shop.example.com/"),
        ("https://cdn.example.com/static/app.3f9a.js", "https://example.com/"),
        ("https://fonts.gstatic.com/s/inter/v12/abc.woff2", "https://example.com/"),
        ("https://www.google-analytics.com/analytics.js", "https://blog.example.com/"),
        ("https://images.example.com/hero.jpg", "https://example.com/"),
        ("https://securepubads.g.doubleclick.net/tag/js/gpt.js", "https://news.example.com/"),
    ];
    let n = 200_000;
    let t = Instant::now();
    let mut blocked = 0;
    for i in 0..n {
        let (u, s) = urls[i % urls.len()];
        if cached.check(u, s, None, false) == Decision::Block {
            blocked += 1;
        }
    }
    let per = t.elapsed() / n as u32;

    println!("filter lines        {rules}");
    println!("build from lists    {build:?}");
    println!("serialized size     {:.1} MB", bytes.len() as f64 / 1_048_576.0);
    println!("load from cache     {load:?}");
    println!("avg check           {per:?}");
    println!("blocked             {blocked}/{n}");
    for (u, s) in urls {
        println!("  {:5}  {u}", format!("{:?}", cached.check(u, s, None, false)));
    }
}
