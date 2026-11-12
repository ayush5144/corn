# 04 — Brand

> **For developers and AI Agents-Claude and Codex etc.** This file helps developers understand the code. The code is always the source of truth: if this file and the code disagree, trust the code. This file may be out of date.
>
> **Created:** 2026-11-11 · **Last updated:** 2026-11-12, 11:28 IST

The browser's name, logo, colours, fonts, voice and copy. Source: the brand board [`mockups/flush/index.html`](../../mockups/flush/index.html) and [decision log](02-decision-log.md) Round 22. Words for product concepts (Flush, Flush Button, Vault…) are in the [glossary](../../GLOSSARY.md).

## Name

**Flush**; **Flush Browser** in full where context is needed (store title, Product Hunt, press). Home-screen label: "Flush". Wordmark: lowercase **flush** in Fredoka 600.

Why (Round 22): the name is the main action and explains itself in public; it frames Flush as private browsing for anything, not only adult sites; it lasts as long as the Flush Button does; flushbrowser.com was free on 2026-11-11. The browser was called Corn until 2026-11-11.

Writing rules:
- The app is named after its main action, and feature names don't change: Flush, Flush Button, Auto-Flush, Kept Session, Keep List, Vault, Collection.
- Write "the Flush Button", never "Flush's Flush Button". When "Flush" could mean the app or the action, say "the app" ("when you leave the app").
- App-wide names in code are brand-neutral (`AppController`, `AppSettings`, `BlockEngine`) and names pages can see carry no brand (`pageBridge`, `loginBridge`), so sites can't spot the browser by name. See [05-rename-to-flush](../03-planning/05-rename-to-flush.md).

## Logo: the Vortex

The board's **A · The Vortex** (original version): four arms of electric blue and cyan spiralling into a drain, two bubbles escaping. The flush seen from above, with no toilet in sight; discreet on a home screen (it looks like a weather app or a game).

| File | What |
|---|---|
| `brand/flush-logo-1024.png` | Master, 1024 px, on the navy background |
| `brand/flush-logo-transparent-1024.png` | Master, 1024 px, transparent background (new-tab page, onboarding) |
| `android/app/src/main/res/drawable-nodpi/flush_logo.png` | In-app logo |
| `android/app/src/main/res/mipmap-*/ic_launcher*` | Launcher icons: adaptive (art on #071226, inside Android's 66 dp safe zone so circle and squircle launchers don't clip it) plus legacy sizes |

The art is an AI draft (Codex image generation); a clean vector redraw is on the backlog. Check it at 48 px on a real phone: the board noted the bubbles and highlights blur at that size. The earlier logos stay in `brand/` as a record and are no longer used.

## Nav bar and the Flush Button

Left to right (founder, 2026-11-12: back to the layout from before the rename, for now):

**Back** · **Vault** · **Tabs** · **Flush** · **Menu**

- The Flush Button is a filled **Fill-blue** button, second from the right, with the swirl glyph in Ink (`Glyphs.Flush`).
- **Parked:** the raised centre button with the "Riding the swirl" drop (Round 22, built 2026-11-11 in commit 1cb9f0b). The master stays in `brand/flush-button-wink-1024.png`; the app file was removed.

## Colours

Same token names as `ui/Theme.kt`. Navy-ink backgrounds, cyan text accents, one bright blue for every action. **Fill** and **Ink** are the same in both themes, so the Flush Button never changes. Buttons use dark Ink on Fill: white on the Fill blue would only reach about 2.4 : 1.

| Token | Dark (default) | Light |
|---|---|---|
| Bg | `#071226` | `#F5FAFF` |
| Surface | `#0D1C36` | `#E8F1FC` |
| Surface2 | `#142749` | `#DAE7F7` |
| Line | `#24395F` | `#C3D5EC` |
| Text | `#EAF4FF` | `#0A1A30` |
| Muted | `#93A8C6` | `#4D6280` |
| Accent | `#3FD8FF` | `#005FCC` |
| Fill | `#1EA7FF` | `#1EA7FF` |
| Ink | `#03142B` | `#03142B` |
| Success | `#3DDC97` | `#11804E` |

**Contrast** (WCAG 2.x, computed by `mockups/flush/contrast.py`; every text pair is at least 4.5 : 1):

| Pair | Dark | Light |
|---|---|---|
| Text / Bg | 16.81 : 1 AAA | 16.62 : 1 AAA |
| Muted / Bg | 7.71 : 1 AAA | 5.93 : 1 AA |
| Accent / Bg | 11.11 : 1 AAA | 5.70 : 1 AA |
| Ink / Fill | 7.05 : 1 AAA | 7.05 : 1 AAA |
| Success / Bg | 10.58 : 1 AAA | 4.74 : 1 AA |

## Fonts

| Font | Used for | Licence |
|---|---|---|
| **Fredoka** | Display: the **flush** wordmark (600), headings. Rounded and bubbly, like letters made of water. Replaces Bricolage Grotesque | SIL OFL 1.1 (google/fonts) |
| **Figtree** | All UI text (unchanged) | SIL OFL 1.1 (google/fonts) |

Both are bundled in the app with their licence texts in `assets/licenses/` and listed in [02-open-source](../04-reference/02-open-source.md).

## Voice

- Cool, quick, cheeky, tasteful (unchanged by the rename).
- Jokes are about **your tracks going down the drain**, never about what's in the bowl. Nothing crude, gross, sexual or medical.
- Water reads as clean, not dirty, so the toilet humour stays a wink.
- The old cob, kernel and 🌽 jokes are gone; use the lines below.

## Copy kit

**Taglines** (★ = top pick)
- Browse whatever. Flush whenever. ★ **primary**
- One tap. No trace. ★
- Browse. Save what matters. Flush the rest. ★
- Your history, down the drain.
- What happens in Flush, swirls away.
- Go ahead. We'll flush.
- Private browsing, with a handle.
- Clean sessions. Clear conscience.

**After a Flush** (toast one-liners; the app's list is in `browser/Flusher.kt`)

A line with two sentences shows **one sentence per line**, each line centred on the new-tab page with the ✓ before the first, ("All gone." / "Even we don't know what it was."), in toasts and after a Flush: `ui/Copy.kt` (`Copy.oneSentencePerLine`, unit-tested). A sentence ending before an emoji stays on one line ("Swirl, swirl, gone. 🌀").
- Swirl, swirl, gone. 🌀
- Flushed. Nobody saw nothing.
- Squeaky clean. Like it never happened.
- Session down the drain. Your Vault's safe.
- Whoosh. Clean slate.
- Gone with the swirl.
- All gone. Even we don't know what it was.
- Done. Feel free to wash your hands of it.
- That's the sound of zero tabs.
- Poof. Sparkling. ✨

**Onboarding** (`ui/OnboardingScreen.kt`)
1. Browse whatever. Flush whenever.
2. Every session starts empty. No history waiting for you.
3. One tap of the Flush Button wipes your tabs, cookies and history.
4. Your Vault keeps only what you choose, locked, on this phone.
5. Nothing you browse leaves this phone. No account needed.
6. Button: I'm 18 or older · Start browsing

**Empty states**

| Screen | Line |
|---|---|
| History | Nothing here. Freshly flushed. 🌀 |
| Tabs | No tabs. Just calm water. |
| Vault | Your Vault's empty. Save something worth keeping. |
| Settings (Flush timing) | Default. Nothing goes until you pull the handle. |

**App store**
- Title: Flush Browser
- Short description (80/80 characters): "Private browser. Block trackers, keep a locked Vault, flush the rest in one tap."
- Shorter alternative (72): "Private browser that blocks trackers and flushes your tracks in one tap."

## Before public launch

- Trademark search for "Flush" (USPTO / EUIPO) and social handles: not done yet.
- Vector redraw of the Vortex.
- See [TRADEMARKS.md](../../TRADEMARKS.md): the name, logo and visual identity aren't covered by the code licence.
