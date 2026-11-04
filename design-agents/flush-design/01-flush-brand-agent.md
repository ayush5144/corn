# Agent: Flush brand exploration (logos, colour theme, copy kit)

- **Run on:** 2026-11-05, as a Claude Code subagent (`general-purpose`), in the background.
- **Tools it used:** Bash (Codex CLI image generation, a Python WCAG contrast script, headless Chrome), Read, Write.
- **Inspiration given:** a stock "flush toilet" icon the founder liked (bright-blue swirl in a bowl, a finger pressing the flush button). It was shown for mood only: never traced, never passed to the image model.
- **Output:** `mockups/flush/index.html` (brand board), `mockups/flush/flush-a-vortex.png`, `flush-b-button.png`, `flush-c-wink.png`, `mockups/flush/contrast.py`. Full-size originals: `~/Documents/corn-logo-originals/flush/`.
- **Result:** 3 generations; recommended A (Vortex). Palette (dark / light), Fredoka + Figtree, 8 taglines, 10 post-Flush one-liners, onboarding, empty states, store line.

## Exact brief given to the agent

The image path below pointed to a converted copy of the founder's reference image in that session's temporary folder.

```text
You are the brand designer for a possible RENAME of a browser app from "Corn" to "Flush". Nothing is decided; you are producing an exploration for the founder to compare. Do NOT change any app code, anything in ~/dev/corn/brand/, ~/dev/corn/mockups/logos/ (another agent works there), or other mockups. Do not commit to git.

## The product (understand the goal)
A free, private, mobile-first Android browser for adults (18+). Promise: "use whenever you want" — every session can start empty, one tap of the **Flush Button** wipes all browsing (tabs, cookies, history), a locked **Vault** keeps only what you choose to save, ads/trackers/pop-ups are blocked, nothing about your browsing leaves the phone. Personality: cool, funny, modern, a bit quirky and cheeky/naughty-with-a-wink — but tasteful: playful toilet humour about *flushing away your tracks* is fine; nothing crude, gross, sexual or medical (no poop, no diarrhea, no bodily fluids). Under the name "Corn" the brand already has this voice. Existing Corn copy, to match in spirit:
- Tagline: "Browse. Save what matters. Flush the rest."
- Onboarding: "The private browser that keeps you organised." / "Nothing you browse leaves this phone. No account needed." / "I'm 18 or older · Start browsing"
- Flush one-liners shown after a Flush: "All gone. Fresh as a new cob. 🌽" · "Flushed. Nobody saw nothing." · "Squeaky clean. Like it never happened." · "Session down the drain. Your Vault's safe." · "Poof. Clean slate, same great corn."
- Settings: "Default. Nothing goes until you hit the toilet." Empty history: "Nothing here. Fresh as a new cob. 🌽"

## The founder's visual direction for Flush
The water action — the swirling tornado/vortex of water inside a flushing toilet — in **bright blue**: cool, quirky, funny. Reference for mood ONLY: <path to the reference image> (a stock icon: toilet tank, a finger pressing the flush button, cyan swirling water in the bowl, thick black outlines, flat colours). You may Read it to understand the mood, but it is someone else's copyrighted stock art: never trace or replicate it, never pass it to the image model, create original work.

## Deliverables (all in ~/dev/corn/mockups/flush/)
1. **2–3 distinct logo concepts**, e.g. (a) a pure vortex/swirl mark (water spiralling into a drain — abstract, iconic), (b) a playful mark combining a press/flush button or toilet-bowl silhouette with the bright-blue swirl, (c) your own surprise idea (e.g. a wink, a water-drop character, an "F" made of a swirl). Flat, modern app-icon style, readable at 48px, no text inside icons. Generate with Codex image generation (tested, works), from the folder:
   cd ~/dev/corn/mockups/flush && codex exec --skip-git-repo-check -s workspace-write "Use your image generation tool to create <detailed prompt>. 1024x1024 PNG. Save it in the current directory as <name>.png and print its absolute path."
   Each call takes ~1 minute: use a Bash timeout of at least 400000 ms. Look at every PNG with the Read tool; refine and regenerate poor ones. Max ~9 generations total.
2. **Colour theme** for dark (default) and light mode, as tokens matching the app's existing token names: Bg, Surface, Surface2, Line, Text, Muted, Accent (text/icons), Fill (buttons, the Flush button), Ink (text on Fill), Success. Bright blue/cyan led. Compute WCAG contrast ratios for Text/Bg, Muted/Bg, Accent/Bg, Ink/Fill in both themes (write a small python script; every text pair must be ≥ 4.5:1) and show them.
3. **Copy kit** in Flush's voice: 6–8 tagline options, 8–10 post-Flush one-liners (replacing the corn jokes), onboarding headline + 3 bullets, 3 empty-state lines, the app-store short description (≤80 characters). Cheeky, quirky, tasteful as above.
4. **One HTML brand board**: ~/dev/corn/mockups/flush/index.html — plain HTML, mobile width (max-width ~420px centred), self-contained except Google Fonts (pick fonts that fit; say why). Sections: concept rationale; each logo concept large + as Android squircle app icon on dark and light + small sizes 192/96/48 + lockup with the wordmark "flush"; a fake Android home-screen grid with neutral CSS placeholder apps; the palette swatches with hex + contrast results (dark and light); a phone-screen mock of the browser's new-tab page restyled in the Flush theme (address bar "Search or type a site", a stats card "Trackers & ads blocked", bottom toolbar with a big Flush button) with fake neutral content; the copy kit. Use only fake neutral sites; no real brand logos; no adult content.

When done, reply with: the HTML path, all PNG paths, which logo concept you recommend and why (2–3 sentences), the palette hex values, your top 3 taglines, and any problems.
```
