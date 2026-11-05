# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at https://mozilla.org/MPL/2.0/.
"""The emulator, seen through adb: the UI tree, taps, the foreground app, screen pixels.

Stdlib only. Used by scripts/qa/run.py; also a small CLI for exploring by hand:

    python3 -I scripts/qa/device.py tree          # print the UI tree
    python3 -I scripts/qa/device.py tap TEXT      # tap the first node whose text/desc/id matches
    python3 -I scripts/qa/device.py top           # the foreground app
"""
import os
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

ADB = os.environ.get("ADB") or os.path.expanduser("~/Library/Android/sdk/platform-tools/adb")
APP = "app.cornbrowser"


def adb(*args, check=False, timeout=30):
    r = subprocess.run([ADB, *args], capture_output=True, timeout=timeout)
    if check and r.returncode != 0:
        raise RuntimeError(f"adb {' '.join(args)} failed: {r.stderr.decode(errors='replace')}")
    return r.stdout.decode(errors="replace")


def adb_bytes(*args, timeout=30):
    return subprocess.run([ADB, *args], capture_output=True, timeout=timeout).stdout


# ---------------- UI tree (uiautomator) ----------------

class Node:
    """One node of the uiautomator dump. `in_web` is True for nodes inside a WebView (page content)."""

    def __init__(self, el, in_web):
        self.el = el
        self.cls = el.get("class") or ""
        self.text = el.get("text") or ""
        self.desc = el.get("content-desc") or ""
        self.rid = el.get("resource-id") or ""
        self.clickable = el.get("clickable") == "true"
        b = list(map(int, re.findall(r"-?\d+", el.get("bounds") or "[0,0][0,0]")))
        self.bounds = tuple(b)
        self.in_web = in_web

    @property
    def center(self):
        x1, y1, x2, y2 = self.bounds
        return ((x1 + x2) // 2, (y1 + y2) // 2)

    def label(self):
        return self.text or self.desc

    def __repr__(self):
        return f"<{self.cls.split('.')[-1]} {self.label()!r} id={self.rid!r} {self.bounds}>"


def ui_nodes():
    """All nodes on screen, in document order. Retries: uiautomator fails while the UI animates."""
    for _ in range(4):
        out = adb("shell", "uiautomator dump /sdcard/corn-qa-ui.xml")
        xml = adb_bytes("exec-out", "cat", "/sdcard/corn-qa-ui.xml")
        if b"<hierarchy" in xml and "dumped" in out.lower():
            break
        time.sleep(0.5)
    else:
        return []
    root = ET.fromstring(xml)
    nodes = []

    def walk(el, in_web):
        if el.tag == "node":
            n = Node(el, in_web)
            nodes.append(n)
            in_web = in_web or n.cls == "android.webkit.WebView"
        for c in el:
            walk(c, in_web)

    walk(root, False)
    return nodes


def app_texts(nodes=None):
    """Text shown by Corn itself (sheets, bars, toasts), not by web pages."""
    nodes = ui_nodes() if nodes is None else nodes
    return [n.label() for n in nodes if not n.in_web and n.cls != "android.webkit.WebView" and n.label()]


def find(nodes, text=None, rid=None, exact=True, web=None):
    for n in nodes:
        if web is not None and n.in_web != web:
            continue
        if rid is not None and n.rid != rid:
            continue
        if text is not None:
            lab = n.label()
            if not (lab == text if exact else text in lab):
                continue
        return n
    return None


def tap(x, y):
    adb("shell", "input", "tap", str(int(x)), str(int(y)))


def long_press(x, y, ms=1200):
    adb("shell", "input", "swipe", str(int(x)), str(int(y)), str(int(x)), str(int(y)), str(ms))


def key(code):
    adb("shell", "input", "keyevent", str(code))


def back():
    key(4)


def input_text(s):
    # `input text` needs spaces as %s; QA URLs never contain spaces. Sent in short pieces with a
    # pause: one long burst arrives faster than Corn's address bar (and its suggestion list)
    # redraws, and characters went missing (always late in the address).
    s = s.replace(" ", "%s").replace("'", "")
    for i in range(0, len(s), 8):
        adb("shell", "input text '" + s[i:i + 8] + "'")
        time.sleep(0.15)


# ---------------- apps and activities ----------------

def foreground():
    """The package/activity Android shows on top, e.g. 'app.cornbrowser/.MainActivity'."""
    out = adb("shell", "dumpsys activity activities")
    m = re.search(r"topResumedActivity=ActivityRecord\{\S+ \S+ (\S+)", out) or \
        re.search(r"mResumedActivity: ActivityRecord\{\S+ \S+ (\S+)", out)
    return m.group(1) if m else ""


def foreground_package():
    return foreground().split("/")[0]


def resolve_package(uri):
    """The app Android would open for a VIEW of this URI (cmd package resolve-activity)."""
    out = adb("shell", f"cmd package resolve-activity --brief -a android.intent.action.VIEW -d '{uri}'")
    lines = [l.strip() for l in out.splitlines() if "/" in l]
    return lines[-1].split("/")[0] if lines else ""


def open_url(url):
    adb("shell", f"am start -a android.intent.action.VIEW -d '{url}' {APP}")


def bring_corn_back():
    adb("shell", f"am start -n {APP}/.MainActivity")


def corn_pid():
    return adb("shell", "pidof", APP).strip().split(" ")[0]


# ---------------- screen pixels ----------------

class Screen:
    """A raw screenshot (adb exec-out screencap, RGBA_8888)."""

    def __init__(self):
        raw = adb_bytes("exec-out", "screencap", timeout=60)
        w = int.from_bytes(raw[0:4], "little")
        h = int.from_bytes(raw[4:8], "little")
        header = len(raw) - w * h * 4
        if w <= 0 or h <= 0 or header not in (12, 16):
            raise RuntimeError(f"unexpected screencap format ({len(raw)} bytes, {w}x{h})")
        self.w, self.h, self.data, self.off = w, h, raw, header

    def pixel(self, x, y):
        i = self.off + (int(y) * self.w + int(x)) * 4
        return tuple(self.data[i:i + 3])


def close_to(rgb, want, tol=12):
    return all(abs(a - b) <= tol for a, b in zip(rgb, want))


def wait_for(fn, timeout=5.0, interval=0.4):
    """Poll fn() until it returns something truthy; returns that value, or the last falsy one."""
    end = time.time() + timeout
    v = fn()
    while not v and time.time() < end:
        time.sleep(interval)
        v = fn()
    return v


if __name__ == "__main__":
    cmd = sys.argv[1] if len(sys.argv) > 1 else "tree"
    if cmd == "tree":
        for n in ui_nodes():
            print(("  [web] " if n.in_web else "  ") + repr(n) + (" clickable" if n.clickable else ""))
    elif cmd == "tap":
        n = find(ui_nodes(), text=sys.argv[2], exact=False) or find(ui_nodes(), rid=sys.argv[2])
        if not n:
            sys.exit(f"not found: {sys.argv[2]}")
        tap(*n.center)
        print("tapped", n)
    elif cmd == "top":
        print(foreground())
    elif cmd == "resolve":
        print(resolve_package(sys.argv[2]))
    else:
        sys.exit(__doc__)
