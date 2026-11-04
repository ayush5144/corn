# Agent: Flush per-concept logo pages (Vortex, Button, Wink)

- **Run on:** 2026-11-05, as a Claude Code subagent (`general-purpose`), in the background, after the brand exploration.
- **Tools it used:** Bash (Codex CLI image generation, `sips`, headless Chrome), Read, Write.
- **Output:** `mockups/flush/vortex.html`, `button.html`, `wink.html`; PNGs in `mockups/flush/vortex/`, `button/`, `wink/` (512 px); full-size originals in `~/Documents/corn-logo-originals/flush/<concept>/`. Added a "Concept pages" card to `mockups/flush/index.html`.
- **Result:** 6 generations. Chosen: Vortex → `vortex-tri.png`, Button → `button-navy.png`, Wink → `wink-original.png`.

## Exact brief given to the agent

```text
You are a brand designer for "Flush", a possible new name for the private Android browser currently called "Corn". An earlier pass made a combined Flush brand board at ~/dev/corn/mockups/flush/index.html with three logo concepts: flush-a-vortex.png (A, Vortex: blue/cyan water arms spiralling into a drain), flush-b-button.png (B, Button: white push button with a whirlpool inside), flush-c-wink.png (C, Wink: winking water-drop character). Read that board first (palette, fonts Fredoka + Figtree, copy kit) and look at the three PNGs with the Read tool.

The Corn concepts each have their own detailed page: look at ~/dev/corn/mockups/logos/flush-kernel.html, corny.html and tab-cob.html (and their folders) to see the format. YOUR TASK: give each Flush concept the same treatment — one page per concept with its own variants.

For EACH of the three concepts (vortex, button, wink):
1. Generate 2 NEW variants of that concept with Codex image generation (so each concept has the original + 2 alternates), staying in the Flush palette (dark: Bg #071226, Fill #1EA7FF, Accent #3FD8FF; Ink #03142B) — flat, modern app-icon style, readable at 48px, no text inside the icon, tasteful (cheeky toilet-flush humour is fine; nothing crude, gross, sexual or medical). Command (tested, works; each call ~1 minute, use a Bash timeout of at least 400000 ms):
   cd ~/dev/corn/mockups/flush/<concept> && codex exec --skip-git-repo-check -s workspace-write "Use your image generation tool to create <detailed prompt>. 1024x1024 PNG. Save it in the current directory as <name>.png and print its absolute path."
   Create each folder first and copy the existing concept PNG into it as the "original". Look at every result with Read; regenerate poor ones. Max ~8 generations in total. After choosing, shrink every PNG in these folders to 512px with: sips -Z 512 <file> (the repo keeps 512px copies), but FIRST copy the full-size new PNGs into ~/Documents/corn-logo-originals/flush/<concept>/.
2. Write ~/dev/corn/mockups/flush/<concept>.html (vortex.html, button.html, wink.html) in the same format as the Corn concept pages, mobile width (max-width ~420px, centred), plain HTML, Google Fonts only: concept name + rationale; chosen logo large; Android squircle app icon on dark and light; a fake home-screen grid with neutral CSS placeholder apps and the icon labelled "Flush"; sizes 192/96/48 (make the sizes row wrap: flex-wrap: wrap, so it never overflows 420px); lockup with the wordmark "flush" in Fredoka; alternates with a one-line critique each; and 3 post-Flush one-liners that suit this concept's personality (from or in the spirit of the board's copy kit). Add a link back to index.html and to ../brand-compare.html.
3. Add a small "Concept pages" link list near the top of ~/dev/corn/mockups/flush/index.html pointing to the three pages (don't otherwise change the board).

Rules: plain HTML mockups in ~/dev/corn/mockups/ only (never hosted artifacts). Don't touch app code, ~/dev/corn/brand/, ~/dev/corn/mockups/logos/, or ~/dev/corn/mockups/brand-compare.html. Don't commit. Check each page renders by taking a headless screenshot at 600px wide: "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome" --headless=new --disable-gpu --hide-scrollbars --window-size=600,2400 --screenshot=/tmp/x.png file://... (headless Chrome's minimum width is above 420, so use 600 and expect the 420px page centred) and Read the screenshot.

Reply with: the three HTML paths, the PNG paths per concept, which variant you chose for each and why (one sentence each), and any problems.
```
