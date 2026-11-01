// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

//! Build-time tool: compile filter lists into a serialized engine for the app's
//! offline starter list. Usage: corn-compile <out.dat> <list.txt>...

use corn_block::Blocker;

fn main() {
    let args: Vec<String> = std::env::args().skip(1).collect();
    assert!(args.len() >= 2, "usage: corn-compile <out.dat> <list.txt>...");
    let lists: Vec<String> = args[1..].iter().map(|p| std::fs::read_to_string(p).expect("read list")).collect();
    let lines: usize = lists.iter().map(|l| l.lines().count()).sum();
    assert!(lines > 10_000, "list looks too small ({lines} lines); refusing to build a starter engine");
    let bytes = Blocker::from_lists(&lists).serialize();
    std::fs::write(&args[0], &bytes).expect("write engine");
    println!("compiled {lines} lines -> {} ({:.1} MB)", args[0], bytes.len() as f64 / 1_048_576.0);
}
