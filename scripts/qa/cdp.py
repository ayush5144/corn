# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at https://mozilla.org/MPL/2.0/.
"""Read page state from Flush's WebViews over Chrome DevTools (debug builds only).

Forwards a local port to Flush's `webview_devtools_remote_<pid>` socket and runs
Runtime.evaluate in the page whose URL contains a given string. Evaluation never sets
`userGesture`, so it can't fake a tap: checks tap with adb and only *read* through here.

    python3 -I scripts/qa/cdp.py pages
    python3 -I scripts/qa/cdp.py eval URL_PART 'document.title'
"""
import base64
import json
import os
import socket
import sys
import urllib.request

sys.dont_write_bytecode = True  # no __pycache__ in the repo
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import device  # noqa: E402

PORT = int(os.environ.get("QA_CDP_PORT", "9334"))
_forwarded_pid = None


def _forward():
    global _forwarded_pid
    pid = device.app_pid()
    if not pid:
        raise RuntimeError("Flush isn't running")
    if pid != _forwarded_pid:
        device.adb("forward", f"tcp:{PORT}", f"localabstract:webview_devtools_remote_{pid}", check=True)
        _forwarded_pid = pid


def pages():
    """Every page Flush's WebViews hold: url, attached/visible and its place on screen (device px)."""
    _forward()
    try:
        raw = json.load(urllib.request.urlopen(f"http://127.0.0.1:{PORT}/json", timeout=10))
    except OSError:
        return []
    out = []
    for p in raw:
        if p.get("type") != "page":
            continue
        try:
            d = json.loads(p.get("description") or "{}")
        except ValueError:
            d = {}
        p["info"] = d
        out.append(p)
    return out


def page(url_part):
    """The page whose URL contains url_part, preferring one attached to a view."""
    matches = [p for p in pages() if url_part in p.get("url", "")]
    matches.sort(key=lambda p: not p["info"].get("attached"))
    return matches[0] if matches else None


class _WS:
    def __init__(self, ws_url):
        path = ws_url.split(str(PORT), 1)[1]
        self.s = socket.create_connection(("127.0.0.1", PORT), timeout=20)
        key = base64.b64encode(os.urandom(16)).decode()
        self.s.sendall((f"GET {path} HTTP/1.1\r\nHost: 127.0.0.1:{PORT}\r\nUpgrade: websocket\r\n"
                        f"Connection: Upgrade\r\nSec-WebSocket-Key: {key}\r\nSec-WebSocket-Version: 13\r\n\r\n").encode())
        buf = b""
        while b"\r\n\r\n" not in buf:
            chunk = self.s.recv(4096)
            if not chunk:
                raise RuntimeError("DevTools closed the connection")
            buf += chunk

    def send(self, obj):
        data = json.dumps(obj).encode()
        mask = os.urandom(4)
        n = len(data)
        if n < 126:
            head = bytes([0x81, 0x80 | n])
        elif n < 65536:
            head = bytes([0x81, 0x80 | 126]) + n.to_bytes(2, "big")
        else:
            head = bytes([0x81, 0x80 | 127]) + n.to_bytes(8, "big")
        self.s.sendall(head + mask + bytes(b ^ mask[i % 4] for i, b in enumerate(data)))

    def _exact(self, n):
        out = b""
        while len(out) < n:
            chunk = self.s.recv(n - len(out))
            if not chunk:
                raise RuntimeError("DevTools closed the connection")
            out += chunk
        return out

    def recv(self):
        h = self._exact(2)
        n = h[1] & 0x7F
        if n == 126:
            n = int.from_bytes(self._exact(2), "big")
        elif n == 127:
            n = int.from_bytes(self._exact(8), "big")
        return json.loads(self._exact(n))

    def close(self):
        self.s.close()


def evaluate(url_part, expr):
    """Run expr in the page whose URL contains url_part; returns its JSON value (or None)."""
    p = page(url_part)
    if not p or not p.get("webSocketDebuggerUrl"):
        return None
    ws = _WS(p["webSocketDebuggerUrl"])
    try:
        ws.send({"id": 1, "method": "Runtime.evaluate",
                 "params": {"expression": expr, "awaitPromise": True, "returnByValue": True}})
        while True:
            m = ws.recv()
            if m.get("id") == 1:
                r = m.get("result", {}).get("result", {})
                return r.get("value")
    except (OSError, RuntimeError):
        return None
    finally:
        ws.close()


def element_center(url_part, selector):
    """Screen point (device px) at the centre of an element, scrolling it into view first."""
    p = page(url_part)
    if not p:
        return None
    r = evaluate(url_part, f"""(() => {{
        const e = document.querySelector({json.dumps(selector)}); if (!e) return null;
        e.scrollIntoView({{block: 'center'}});
        const b = e.getBoundingClientRect();
        return {{x: b.left + b.width / 2, y: b.top + b.height / 2, dpr: devicePixelRatio}};
    }})()""")
    if not r:
        return None
    p = page(url_part)  # re-read: its place on screen may have changed
    return (p["info"].get("screenX", 0) + r["x"] * r["dpr"], p["info"].get("screenY", 0) + r["y"] * r["dpr"])


if __name__ == "__main__":
    if len(sys.argv) > 1 and sys.argv[1] == "eval":
        print(json.dumps(evaluate(sys.argv[2], sys.argv[3]), indent=1))
    else:
        for p in pages():
            print(p["url"], p["info"])
