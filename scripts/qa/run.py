# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at https://mozilla.org/MPL/2.0/.
"""Flush's emulator QA checks. Run through scripts/qa-run.sh, which starts the test server.

    python3 -I scripts/qa/run.py [--list] [CHECK_OR_PREFIX ...]

Each check taps through a real flow with adb and passes only on evidence: the test server's
request log, the foreground app, page state read over DevTools, the UI tree, or screen pixels.
Environment (set by qa-run.sh): QA_BASE (http://10.0.2.2:PORT), QA_SERVER_LOG.
"""
import os
import sys
import time
import traceback

sys.dont_write_bytecode = True  # no __pycache__ in the repo
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import cdp  # noqa: E402
import device as d  # noqa: E402

BASE = os.environ.get("QA_BASE", "http://10.0.2.2:8770")
LOG = os.environ.get("QA_SERVER_LOG", "")
RUN = os.environ.get("QA_RUN") or time.strftime("%H%M%S")
NEW_TABS = os.environ.get("QA_NEW_TABS") == "1"

CHECKS = []


def check(name):
    def wrap(fn):
        CHECKS.append((name, fn))
        return fn
    return wrap


class Fail(Exception):
    pass


# ---------------- evidence helpers ----------------

def requests_for(part):
    """Server log lines (GET requests) whose path contains part."""
    try:
        with open(LOG, encoding="utf-8", errors="replace") as f:
            return [l.strip() for l in f if '"GET ' in l and part in l]
    except OSError:
        return []


def requested(part, timeout):
    return bool(d.wait_for(lambda: requests_for(part), timeout))


def hit(name, r):
    """The unique piece of a qa.js target URL: hit.html?n=<name>&r=<run>."""
    return f"/hit.html?n={name}&r={r}"


def page_nodes_visible(nodes):
    return any(n.cls == "android.webkit.WebView" for n in nodes)


def open_page(path, r):
    """Show BASE/path?r=r in Flush and wait until it has loaded (server log + DevTools).

    The first page opens through Android (VIEW intent → a new Flush tab). Later pages reuse the
    tab already showing a QA page (navigated over DevTools), so a run doesn't pile up tabs.
    QA_NEW_TABS=1 opens every page through Android instead."""
    sep = "&" if "?" in path else "?"
    url = f"{BASE}/{path}{sep}r={r}"
    key = url[len(BASE):]  # unique to this page of this check, e.g. /video.html?pane=A&r=…
    current = None
    if not NEW_TABS:
        current = next((p for p in cdp.pages() if p["info"].get("attached") and p["url"].startswith(BASE)), None)
    if current:
        cdp.evaluate(current["url"], f"location.href = {url!r}; true")
    else:
        d.open_url(url)
    ok = d.wait_for(lambda: (cdp.evaluate(key, "document.readyState") == "complete"
                             and (cdp.page(key) or {}).get("info", {}).get("attached")), 12)
    if not ok:
        raise Fail(f"page didn't load: {url}")
    if not requests_for(key):
        raise Fail(f"server log has no request for {key}")
    time.sleep(0.5)  # let Flush's bars settle before tapping (load is already proven above)
    return key


def tap_element(key, selector):
    c = cdp.element_center(key, selector)
    if not c:
        raise Fail(f"element {selector} not found on page")
    time.sleep(0.3)  # scrollIntoView may have moved the page
    c = cdp.element_center(key, selector)
    d.tap(*c)
    # Evidence the tap landed on the element (qa.js records clicks). A page that navigated
    # away can't answer, so only a page still showing that says "no click" fails here.
    el_id = selector.lstrip("#").split(" ")[0]
    landed = d.wait_for(lambda: cdp.evaluate(key, f"QA.taps.includes({el_id!r})") is not False, 2)
    if not landed:
        raise Fail(f"the tap at {tuple(map(int, c))} didn't reach {selector} (page saw clicks on: {cdp.evaluate(key, 'QA.taps')})")
    return c


def long_press_element(key, selector):
    c = cdp.element_center(key, selector)
    if not c:
        raise Fail(f"element {selector} not found on page")
    time.sleep(0.3)
    c = cdp.element_center(key, selector)
    d.long_press(*c, ms=1200)
    return c


BAR_TEXTS = {"DuckDuckGo"}


def sheet_texts(nodes):
    """Text in a Flush sheet/menu: app text that isn't the address bar, counters or page."""
    out = []
    for n in nodes:
        lab = n.label()
        if n.in_web or not lab or n.cls in ("android.webkit.WebView", "android.widget.EditText"):
            continue
        lab = " ".join(lab.split())
        if lab in BAR_TEXTS or lab.isdigit():
            continue
        out.append(lab)
    return out


def dismiss_sheet_if_any():
    """A sheet hides the page from the UI tree; Back closes it (never pressed when no sheet)."""
    nodes = d.ui_nodes()
    if not page_nodes_visible(nodes) and sheet_texts(nodes):
        d.back()
        time.sleep(0.8)


def menu_button(nodes):
    """Flush's menu: the right-most button of the bottom bar. (Tabs is next to it, and the
    Flush Button next to that, so this checks the menu really opened before anything else is tapped.)"""
    bar = [n for n in nodes if n.clickable and not n.in_web and n.bounds[1] > nodes[0].bounds[3] * 0.85]
    return max(bar, key=lambda n: n.bounds[0]) if bar else None


def open_menu():
    m = menu_button(d.ui_nodes())
    if not m:
        raise Fail("menu button not found")
    d.tap(*m.center)
    nodes = d.wait_for(lambda: (lambda ns: ns if d.find(ns, text="Settings", web=False) else None)(d.ui_nodes()), 4)
    if not nodes:
        raise Fail("menu didn't open")
    return nodes


def tap_menu_item(label):
    nodes = open_menu()
    n = d.find(nodes, text=label, web=False)
    if not n:
        d.back()
        return False
    d.tap(*n.center)
    return True


def ensure_ready():
    """Start each check from Flush in front, no fullscreen, no sheet."""
    if d.foreground_package() != d.APP:
        d.bring_app_back()
        d.wait_for(lambda: d.foreground_package() == d.APP, 5)
    for p in cdp.pages():
        if p["info"].get("attached") and p["url"].startswith(BASE) and cdp.evaluate(p["url"], "!!document.fullscreenElement"):
            d.back()
            time.sleep(1)
    dismiss_sheet_if_any()


def fullscreen(key):
    return bool(cdp.evaluate(key, "!!document.fullscreenElement"))


# ---------------- pop-ups ----------------

@check("popups-tapped-link-opens")
def popups_tapped_link(r):
    key = open_page("popups.html", r)
    tap_element(key, "#blank")
    ok = requested(hit("popup-blank", r), 6)
    return ok, f"tapped target=_blank link; requests for its page: {len(requests_for(hit('popup-blank', r)))}"


@check("popups-tapped-open-opens")
def popups_tapped_open(r):
    key = open_page("popups.html", r)
    tap_element(key, "#open")
    ok = requested(hit("popup-open", r), 6)
    ret = cdp.evaluate(key, "QA.log.join(',')")
    return ok, f"tapped window.open button; requests: {len(requests_for(hit('popup-open', r)))}; window.open returned: {ret}"


@check("popups-no-tap-blocked")
def popups_no_tap(r):
    key = open_page("popups.html?auto=1", r)
    ran = d.wait_for(lambda: cdp.evaluate(key, "window.qaAutoRan === true"), 8)
    if not ran:
        raise Fail("the page's no-tap window.open never ran")
    time.sleep(2)  # give a (wrongly) opened window time to request its page
    n = len(requests_for(hit("popup-auto", r)))
    ret = cdp.evaluate(key, "document.getElementById('auto').textContent")
    return n == 0, f"no-tap window.open ran ({ret!r}); requests for its page: {n} (want 0)"


@check("popups-self-close-returns")
def popups_self_close(r):
    key = open_page("popups.html", r)
    tap_element(key, "#selfclose")
    opened = requested(f"/popup-close.html?r={r}", 6)
    if not opened:
        return False, "tapped pop-up never loaded (0 requests for popup-close.html)"
    closing = requested(hit("popup-close-closing", r), 8)
    if not closing:
        return False, "pop-up loaded but its window.close() timer never ran"

    def back_on_opener():
        ps = cdp.pages()
        opener = next((p for p in ps if p["url"].startswith(f"{BASE}/popups.html?r={r}")), None)
        popup = next((p for p in ps if p["url"].startswith(f"{BASE}/popup-close.html?r={r}")), None)
        return opener and opener["info"].get("attached") and not (popup and popup["info"].get("attached"))
    ok = d.wait_for(back_on_opener, 5)
    shown = [p["url"].replace(BASE, "") for p in cdp.pages() if p["info"].get("attached")]
    return bool(ok), f"pop-up loaded and called window.close(); pages on screen afterwards: {shown}"


@check("popups-burst-one-window")
def popups_burst(r):
    key = open_page("popups.html", r)
    tap_element(key, "#burst")
    requested("n=popup-burst-", 5)
    time.sleep(2)  # let any further windows request their pages
    got = sorted({i for i in (1, 2, 3) if requests_for(hit(f"popup-burst-{i}", r))})
    return len(got) == 1, f"one tap asked for 3 windows; windows that loaded: {got or 'none'} (want exactly 1)"


# ---------------- links to other apps ----------------

def scheme_check(r, link_id, uri):
    want = d.resolve_package(uri)
    if not want:
        raise Fail(f"no app on the emulator handles {uri.split(':')[0]}:")
    key = open_page("schemes.html", r)
    tap_element(key, f"#{link_id}")

    def sheet():
        ns = d.ui_nodes()
        if d.foreground_package() != d.APP:
            return ("left", ns)
        n = d.find(ns, text="Open", web=False) or next(
            (x for x in ns if not x.in_web and x.label().startswith("Open in")), None)
        return ("sheet", ns, n) if n else None
    s = d.wait_for(sheet, 4)
    if not s:
        return False, f"tap did nothing: no sheet, still in Flush (Android would open {want})"
    if s[0] == "left":
        fg = d.foreground_package()
        d.bring_app_back()
        return False, f"opened {fg} without asking first"
    texts = sheet_texts(s[1])
    d.tap(*s[2].center)
    fg = d.wait_for(lambda: (lambda p: p if p == want else None)(d.foreground_package()), 6) or d.foreground_package()
    d.bring_app_back()
    d.wait_for(lambda: d.foreground_package() == d.APP, 5)
    return fg == want, f"sheet {texts}; after Open, foreground app: {fg} (want {want})"


@check("schemes-tel-ask-then-open")
def scheme_tel(r):
    return scheme_check(r, "tel", "tel:+15551234567")


@check("schemes-mailto-ask-then-open")
def scheme_mailto(r):
    return scheme_check(r, "mailto", "mailto:hello@example.com")


@check("schemes-sms-ask-then-open")
def scheme_sms(r):
    return scheme_check(r, "sms", "sms:+15551234567")


@check("schemes-geo-ask-then-open")
def scheme_geo(r):
    return scheme_check(r, "geo", "geo:37.42,-122.08")


@check("schemes-no-tap-ignored")
def scheme_no_tap(r):
    key = open_page("schemes.html?auto=1", r)
    ran = d.wait_for(lambda: cdp.evaluate(key, "window.qaAutoRan === true"), 8)
    if not ran:
        raise Fail("the page's no-tap tel: navigation never ran")
    time.sleep(2)
    fg = d.foreground_package()
    nodes = d.ui_nodes()
    sheet = [t for t in sheet_texts(nodes) if t.startswith("Open")]
    if fg != d.APP:
        d.bring_app_back()
    elif sheet:
        dismiss_sheet_if_any()
    return fg == d.APP and not sheet, f"no-tap location='tel:…' ran; foreground: {fg}; sheet: {sheet or 'none'}"


@check("intent-fallback-loads")
def intent_fallback(r):
    # Flush asks before any link leaves the web (Android 11+ hides most installed apps, so Flush
    # can't tell "not installed" from "hidden"). Tapping Open for an app that isn't installed
    # must fall back to the link's web version.
    key = open_page("schemes.html", r)
    tap_element(key, "#intentfb")
    sheet = d.wait_for(lambda: d.find(d.ui_nodes(), text="Open", web=False), 4)
    if not sheet:
        raise Fail(f"no ask sheet (sheet: {sheet_texts(d.ui_nodes()) or 'none'})")
    d.tap(*sheet.center)
    ok = requested(hit("intent-fallback", r), 6)
    fg = d.foreground_package()
    extra = ""
    if not ok:
        extra = f"; sheet: {sheet_texts(d.ui_nodes()) or 'none'}"
        dismiss_sheet_if_any()
    if fg != d.APP:
        d.bring_app_back()
    return ok, f"tapped intent:// for an app that isn't installed, then Open; fallback requests: {len(requests_for(hit('intent-fallback', r)))}; foreground: {fg}{extra}"


# ---------------- theme ----------------

# The bottom sample sits left of centre: the raised Flush Button covers the middle of the page's
# bottom edge (it rises half its height above the toolbar).
def sample_page(key, fracs=((0.25, 0.85), (0.5, 0.85), (0.75, 0.85), (0.2, 0.95))):
    p = cdp.page(key)
    info = p["info"]
    s = d.Screen()
    pts = []
    for fx, fy in fracs:
        x = info.get("screenX", 0) + info["width"] * fx
        y = info.get("screenY", 0) + info["height"] * fy
        pts.append(s.pixel(min(x, s.w - 1), min(y, s.h - 1)))
    return pts


@check("theme-site-toggle-light")
def theme_toggle(r):
    key = open_page("theme.html", r)
    dark = cdp.evaluate(key, "matchMedia('(prefers-color-scheme: dark)').matches")
    tap_element(key, "#light")
    state = d.wait_for(lambda: (lambda s: s if s and s[0] == "light" else None)(
        cdp.evaluate(key, "[document.documentElement.className, getComputedStyle(document.body).backgroundColor]")), 4)
    if not state:
        raise Fail("tapping the site's light button didn't switch the page to light")
    time.sleep(0.6)
    px = sample_page(key)
    ok = all(d.close_to(p, (255, 255, 255)) for p in px)
    note = "" if dark else " (Flush's pages aren't dark right now, so forced darkening isn't exercised)"
    return ok, f"prefers dark: {dark}; page says {state[0]} / {state[1]}; screen pixels {px} (want ~(255,255,255)){note}"


@check("theme-light-only-page")
def theme_light_only(r):
    key = open_page("theme-light.html", r)
    dark = cdp.evaluate(key, "matchMedia('(prefers-color-scheme: dark)').matches")
    bg = cdp.evaluate(key, "getComputedStyle(document.body).backgroundColor")
    px = sample_page(key)
    ok = all(d.close_to(p, (246, 246, 239)) for p in px)
    note = "" if dark else " (Flush's pages aren't dark right now, so forced darkening isn't exercised)"
    return ok, f"prefers dark: {dark}; page declares {bg}; screen pixels {px} (want ~(246,246,239)){note}"


# ---------------- long-press ----------------

def long_press_sheet(key, selector):
    long_press_element(key, selector)
    nodes = d.wait_for(lambda: (lambda ns: ns if not page_nodes_visible(ns) and sheet_texts(ns) else None)(d.ui_nodes()), 3)
    texts = sheet_texts(nodes) if nodes else []
    if nodes:
        d.back()
        time.sleep(0.8)
    return texts


@check("image-long-press-menu")
def image_menu(r):
    key = open_page("image.html", r)
    plain = long_press_sheet(key, "#plain")
    linked = long_press_sheet(key, "#linkedimg")
    ok = any("Save image" in t for t in plain) and any("Save image" in t for t in linked)
    return ok, f"plain image menu: {plain or 'nothing'}; linked image menu: {linked or 'nothing'} (want 'Save image' in both)"


@check("link-title-not-duration")
def link_title(r):
    key = open_page("links.html", r)
    texts = long_press_sheet(key, "#thumb img")
    ok = bool(texts) and "Sunrise over a quiet lake" in texts and "4:02" not in texts
    return ok, f"long-pressed the video thumbnail link; sheet: {texts[:2] or 'nothing'} (want the title 'Sunrise over a quiet lake', not '4:02')"


# ---------------- fullscreen ----------------

@check("fullscreen-back-exits")
def fullscreen_back(r):
    key = open_page("video.html?pane=single", r)
    tap_element(key, "#fs")
    if not d.wait_for(lambda: fullscreen(key), 5):
        raise Fail(f"video didn't go fullscreen ({cdp.evaluate(key, 'window.qaFsError || null')})")
    time.sleep(1)
    d.back()
    out = d.wait_for(lambda: not fullscreen(key), 4)
    p = cdp.page(key)
    still = bool(p and p["info"].get("attached"))
    fg = d.foreground_package()
    presses = 1
    if not out:  # record whether a second Back gets out (QA report #8)
        d.back()
        presses = 2 if d.wait_for(lambda: not fullscreen(key), 3) else "more than 2"
    return bool(out) and still and fg == d.APP, \
        f"in fullscreen; one Back -> fullscreen {'left' if out else 'still on'} (Backs needed: {presses}); same page on screen: {still}; foreground: {fg}"


def type_in_address_bar(url):
    """Type url into the focused pane's address bar and go. `adb input text` sometimes drops
    characters, so the field is read back and retyped until it holds exactly url."""
    for _ in range(3):
        bar = next((n for n in d.ui_nodes() if n.cls.endswith("EditText") and not n.in_web), None)
        if not bar:
            raise Fail("address bar not found")
        d.tap(*bar.center)
        time.sleep(0.8)
        bar = next((n for n in d.ui_nodes() if n.cls.endswith("EditText") and not n.in_web), bar)
        if bar.text:  # a new tab's bar is empty; after a failed try, select all (Ctrl+A) and delete
            d.adb("shell", "input keycombination 113 29")
            d.key(67)
        d.input_text(url)
        # Typing a long address takes a while: wait for the field to hold all of it (up to 3 s)
        # before judging, instead of reading it back too early.
        def full():
            b = next((n for n in d.ui_nodes() if n.cls.endswith("EditText") and not n.in_web), None)
            return b if b and b.text == url else None
        done = d.wait_for(full, 3)
        bar = done or next((n for n in d.ui_nodes() if n.cls.endswith("EditText") and not n.in_web), None)
        if done:
            d.key(66)
            return
    raise Fail(f"couldn't type the address (field shows {bar.text if bar else None!r})")


GREEN = (46, 125, 50)  # video.html's background, #2e7d32


@check("fullscreen-split-pane")
def fullscreen_split(r):
    nodes = open_menu()
    if d.find(nodes, text="Close split screen", web=False):
        d.tap(*d.find(nodes, text="Close split screen", web=False).center)
        time.sleep(1)
    else:
        d.back()
        time.sleep(0.6)
    key_a = open_page("video.html?pane=A", r)
    if not tap_menu_item("Split screen"):
        raise Fail("menu has no Split screen")
    nt = d.wait_for(lambda: [n for n in d.ui_nodes() if n.label() == "New tab" and not n.in_web], 4)
    if not nt:
        raise Fail("split screen tab picker didn't open")
    d.tap(*nt[-1].center)  # the picker's "New tab" button (last on screen)
    time.sleep(1.5)
    key_b = f"/video.html?pane=B&r={r}"
    type_in_address_bar(BASE + key_b)
    try:
        if not d.wait_for(lambda: cdp.evaluate(key_b, "document.readyState") == "complete"
                          and cdp.page(key_a)["info"].get("attached"), 10):
            raise Fail("couldn't get two panes on screen")
        time.sleep(0.8)
        tap_element(key_b, "#fs")
        if not d.wait_for(lambda: fullscreen(key_b), 5):
            raise Fail("pane B's video didn't go fullscreen")
        time.sleep(1.2)
        a = cdp.page(key_a)["info"]
        s = d.Screen()
        x = a.get("screenX", 0) + 10  # the page's left margin is plain background
        ys = [a.get("screenY", 0) + a["height"] * f for f in (0.1, 0.3, 0.5, 0.7, 0.9)]
        px = [s.pixel(x, min(y, s.h - 1)) for y in ys]
        green = sum(d.close_to(p, GREEN, 25) for p in px)
        top = d.adb("shell", "dumpsys activity top")
        webviews = [l for l in top.splitlines() if "android.webkit.WebView{" in l]
        return green == len(px), (f"pane B fullscreen; pane A ({a.get('screenY')}..{a.get('screenY', 0) + a['height']}px) "
                                  f"pixels {px}: {green}/{len(px)} show its page (want all ~{GREEN}); WebViews in view tree: {len(webviews)}")
    finally:
        if fullscreen(key_b):
            d.back()
            d.wait_for(lambda: not fullscreen(key_b), 4)
        time.sleep(0.6)
        tap_menu_item("Close split screen")
        time.sleep(0.8)


# ---------------- runner ----------------

def main(argv):
    if "--list" in argv:
        for name, _ in CHECKS:
            print(name)
        return 0
    wanted = [a for a in argv if not a.startswith("-")]
    chosen = [(n, f) for n, f in CHECKS if not wanted or any(n == w or n.startswith(w) for w in wanted)]
    if not chosen:
        print(f"No check matches {wanted}. Known checks:")
        for name, _ in CHECKS:
            print("  " + name)
        return 2
    if not cdp.pages() and not d.app_pid():
        d.bring_app_back()
        time.sleep(2)
    fails = 0
    for i, (name, fn) in enumerate(chosen, 1):
        r = f"{RUN}-{i:02d}"  # fixed width, so no run id is a prefix of another
        try:
            ensure_ready()
            ok, data = fn(r)
            status = "PASS" if ok else "FAIL"
        except Fail as e:
            status, data = "FAIL", f"setup: {e}"
        except Exception as e:  # noqa: BLE001 - a crashed check is a failed check
            status, data = "FAIL", f"error: {e!r}"
            if os.environ.get("QA_DEBUG"):
                traceback.print_exc()
        fails += status == "FAIL"
        print(f"{status}  {name}  {data}", flush=True)
    print(f"\n{len(chosen) - fails}/{len(chosen)} passed")
    return 1 if fails else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
