/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package com.flushbrowser.passwords

/**
 * Flush's own small login-form script (no third-party library), injected at document start in
 * every frame next to a `loginBridge` WebMessageListener (see WebTab.installLoginBridge).
 *
 * Page → app messages:
 * - `{t:"form"}`: a visible password field appeared (Flush may offer to fill).
 * - `{t:"login", u, p}`: the user submitted a login (form submit, Enter, or tapping the form's button).
 *
 * App → page message: `{t:"fill", u, p}`, sent only after the user taps "Fill" in Flush.
 * Values are set with the native `value` setter (captured before page scripts run) and followed by
 * `input` and `change` events, so React/Vue forms notice.
 *
 * Only runs on https pages (plus local test pages over http in debug builds). Flush checks the
 * origin and frame again on its side; the script is not trusted.
 */
object LoginScript {
    fun source(allowLocalHttp: Boolean): String = JS.replace("__ALLOW_LOCAL__", if (allowLocalHttp) "true" else "false")

    private val JS = """
        (function () {
          if (window.__loginBridge || !window.loginBridge) return;
          window.__loginBridge = true;
          var bridge = window.loginBridge;
          var local = __ALLOW_LOCAL__ && location.protocol === 'http:' &&
            /^(localhost|127\.0\.0\.1|10\.0\.2\.2)$/.test(location.hostname);
          if (location.protocol !== 'https:' && !local) return;

          // Captured now, before page scripts can replace them.
          var desc = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value');
          var setValue = desc && desc.set, getValue = desc && desc.get;
          var post = bridge.postMessage.bind(bridge);
          var USER_TYPES = { text: 1, email: 1, tel: 1, '': 1 };

          function val(el) { return getValue ? getValue.call(el) : el.value; }
          function type(el) { return (el.getAttribute('type') || 'text').toLowerCase(); }
          function visible(el) {
            if (!el || el.disabled || type(el) === 'hidden') return false;
            if (!el.getClientRects().length) return false;
            var s = getComputedStyle(el);
            return s.visibility !== 'hidden' && s.display !== 'none';
          }
          // Fields that were ever password fields: a "show password" button turns them into
          // type="text", but they're still the password (tested: without this, no save offer).
          var seenPw = new WeakSet();
          function remember(scope) {
            Array.prototype.forEach.call((scope || document).querySelectorAll('input[type=password]'), function (el) { seenPw.add(el); });
          }
          function isPw(el) { return !!el && el.tagName === 'INPUT' && (type(el) === 'password' || seenPw.has(el)); }
          function passwordFields(scope) {
            remember(scope);
            return Array.prototype.filter.call((scope || document).querySelectorAll('input'), function (el) { return isPw(el) && visible(el); });
          }
          function hasPw(scope) { return passwordFields(scope).length > 0; }
          document.addEventListener('focusin', function (e) { if (type(e.target) === 'password') seenPw.add(e.target); }, true);
          document.addEventListener('input', function (e) { if (e.target && type(e.target) === 'password') seenPw.add(e.target); }, true);
          // The username: autocomplete="username" in the same form, else the nearest visible
          // text/email/tel field before the password field.
          function usernameFor(pw) {
            var scope = pw.form || document;
            var named = scope.querySelector('input[autocomplete~="username"]');
            if (named && named !== pw && visible(named)) return named;
            var inputs = scope.querySelectorAll('input'), found = null;
            for (var i = 0; i < inputs.length; i++) {
              var el = inputs[i];
              if (el === pw) break;
              if (USER_TYPES[type(el)] && !seenPw.has(el) && visible(el)) found = el;
            }
            return found;
          }

          // ---- capture a submitted login ----
          var lastKey = '', lastAt = 0;
          function capture(scope) {
            var pws = passwordFields(scope).filter(function (f) { return val(f); });
            if (!pws.length) return;
            // Sign-up / change-password forms: "new" and "confirm" fields hold the same value; save that.
            var pw = pws[0];
            if (pws.length >= 2 && val(pws[pws.length - 1]) === val(pws[pws.length - 2])) pw = pws[pws.length - 1];
            var user = usernameFor(pws[0]);
            var u = user ? String(val(user) || '').trim() : '', p = String(val(pw));
            var key = u + '\u0000' + p, now = Date.now();
            if (key === lastKey && now - lastAt < 4000) return; // submit + click fire together
            lastKey = key; lastAt = now;
            post(JSON.stringify({ t: 'login', u: u, p: p }));
          }
          document.addEventListener('submit', function (e) {
            var f = e.target;
            if (f && f.tagName === 'FORM' && hasPw(f)) capture(f);
          }, true);
          document.addEventListener('keydown', function (e) {
            if (e.key !== 'Enter' || !e.target || e.target.tagName !== 'INPUT') return;
            var t = e.target, scope = t.form || document;
            if (isPw(t) || passwordFields(scope).length) capture(t.form || null);
          }, true);
          document.addEventListener('click', function (e) {
            var b = e.target && e.target.closest && e.target.closest('button,input[type=submit],input[type=image],[role=button]');
            if (!b) return;
            // Not "show password" eye buttons: tapping one mid-typing isn't a login.
            var label = ((b.getAttribute('aria-label') || '') + ' ' + (b.textContent || '') + ' ' + (b.className || '')).slice(0, 200);
            if (/show|hide|reveal|visib|toggle|eye/i.test(label)) return;
            var f = b.form || (b.closest && b.closest('form'));
            if (f && hasPw(f)) {
              if (b.tagName === 'BUTTON' && b.type !== 'submit') return; // type="button": not the form's submit
              capture(f);
              return;
            }
            // Forms without a <form> tag: one visible, filled password field on the page.
            if (!f) { var pws = passwordFields(document); if (pws.length === 1 && val(pws[0])) capture(null); }
          }, true);

          // ---- tell the app when a login form is on the page ----
          var reported = false, timer = null;
          function check() {
            timer = null;
            var has = passwordFields(document).length > 0;
            if (has && !reported) post(JSON.stringify({ t: 'form' }));
            reported = has;
          }
          function soon() { if (!timer) timer = setTimeout(check, 300); }
          function boot() {
            check();
            new MutationObserver(soon).observe(document.documentElement, { childList: true, subtree: true, attributes: true, attributeFilter: ['type', 'style', 'class', 'hidden'] });
          }
          if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', boot); else boot();

          // ---- fill, only when the app asks (after the user's tap) ----
          function set(el, v) {
            el.focus();
            if (setValue) setValue.call(el, v); else el.value = v;
            el.dispatchEvent(new Event('input', { bubbles: true }));
            el.dispatchEvent(new Event('change', { bubbles: true }));
          }
          bridge.onmessage = function (e) {
            var m; try { m = JSON.parse(e.data); } catch (_) { return; }
            if (!m || m.t !== 'fill') return;
            var pw = passwordFields(document)[0];
            if (!pw) return;
            var user = usernameFor(pw);
            if (user && m.u) set(user, m.u);
            set(pw, m.p || '');
            lastKey = (m.u || '') + '\u0000' + (m.p || ''); lastAt = Date.now() + 600000; // don't offer to re-save what the app filled
          };
        })();
    """.trimIndent()
}
