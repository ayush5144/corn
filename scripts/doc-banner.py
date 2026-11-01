#!/usr/bin/env python3
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at https://mozilla.org/MPL/2.0/.
"""Add or refresh the developer banner on every doc (docs/**/*.md and GLOSSARY.md).

Created = the commit that first added the file (following renames).
Last updated = the last commit that changed it, or now if it has uncommitted changes.
Times are shown in IST (UTC+05:30). Run from the repo root: python3 scripts/doc-banner.py
"""
import datetime, pathlib, re, subprocess

IST = datetime.timezone(datetime.timedelta(hours=5, minutes=30))
LEAD = "> **For developers and AI Agents-Claude and Codex etc.**"
TEXT = (LEAD + " This file helps developers understand the code. The code is always the source of truth: "
        "if this file and the code disagree, trust the code. This file may be out of date.")

def git(*args):
    return subprocess.run(["git", *args], capture_output=True, text=True).stdout.strip()

def when(iso):
    return datetime.datetime.fromisoformat(iso).astimezone(IST)

BANNER_RE = re.compile(r"\n?> \*\*For developers and AI Agents[^\n]*\n>\n> \*\*Created:\*\*[^\n]*\n")

def without_banner(text):
    return re.sub(r"\s+", " ", BANNER_RE.sub("\n", text, count=1)).strip()

def banner_for(path):
    added = git("log", "--diff-filter=A", "--follow", "--format=%aI", "--", path).splitlines()
    # Edited since the last commit? A change to the banner alone doesn't count.
    committed = subprocess.run(["git", "show", f"HEAD:{path}"], capture_output=True, text=True)
    dirty = committed.returncode != 0 or without_banner(committed.stdout) != without_banner(pathlib.Path(path).read_text())
    now = datetime.datetime.now(IST)
    created = when(added[-1]) if added else now
    last = git("log", "-1", "--format=%aI", "--", path)
    updated = now if dirty or not last else when(last)
    return f"{TEXT}\n>\n> **Created:** {created:%Y-%m-%d} · **Last updated:** {updated:%Y-%m-%d, %H:%M} IST"

def apply(path):
    p = pathlib.Path(path)
    s = p.read_text()
    # Drop an existing banner (the block that starts with LEAD).
    s = BANNER_RE.sub("\n", s, count=1)
    lines = s.split("\n")
    at = 1 if lines and lines[0].startswith("# ") else 0
    rest = "\n".join(lines[at:]).lstrip("\n")
    head = "\n".join(lines[:at])
    b = banner_for(path)
    out = (head + "\n\n" if head else "") + b + "\n\n" + rest
    if out != p.read_text():
        p.write_text(out)

files = sorted(str(f) for f in pathlib.Path("docs").rglob("*.md")) + ["GLOSSARY.md"]
for f in files:
    apply(f)
print(f"banner checked on {len(files)} files")
