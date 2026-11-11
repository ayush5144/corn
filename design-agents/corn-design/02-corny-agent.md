# Agent: Corn logo concept 2, "Corny"

Corn was renamed Flush on 2026-11-11 (decision log Round 22); this record keeps the old name.

- **Run on:** 2026-11-05, as a Claude Code subagent (`general-purpose`), in the background, in parallel with concepts 1 and 3.
- **Tools it used:** Bash (Codex CLI image generation, `sips`), Read (to look at each PNG), Write.
- **Output:** `mockups/logos/corny.html` and `mockups/logos/corny/*.png` (chosen: `corny-shh-cob.png`). Full-size originals: `~/Documents/corn-logo-originals/logos/corny/`.
- **Result:** 4 generations; chose the winking "shh" cob. Afterwards the main session made the page's sizes row wrap (`flex-wrap: wrap`) so it fits 420 px.

## Exact brief given to the agent

```text
You are designing ONE logo concept for "Corn", a free, private, mobile-first Android browser for adults whose personality is "cool, funny, modern". Its signature feature is the **Flush Button**: one tap wipes all browsing (tabs, cookies, history) like a toilet flush. The current logo (~/dev/corn/brand/cornbrowser-logo-square-transparent-1024.png) is a glossy realistic corn cob; the founder says it's "too real". Do NOT modify anything in ~/dev/corn/brand/.

YOUR CONCEPT: "Corny" — a cheeky mascot. A single stylised corn kernel (or a tiny chunky cob) with a face and attitude: e.g. a knowing wink, a smirk, sunglasses, or a finger-to-lips "shh" (privacy!). Sticker/badge style: thick confident outlines, flat colours, playful, modern — think Duolingo/Discord-level character design, not realistic and not childish. Must still read at 48px. No text inside the icon. Palette: corn yellow #FFC83D, dark ink #2A1E00, near-black #0F0E0B, optional leaf green #86C64A. Surprise us — be funny and creative within this direction. Keep it tasteful (no adult or crude content).

HOW TO GENERATE IMAGES (tested, works): run Codex's image generation from the shell, e.g.
  cd ~/dev/corn/mockups/logos/corny && codex exec --skip-git-repo-check -s workspace-write "Use your image generation tool to create <detailed prompt>. 1024x1024 PNG. Save it in the current directory as <name>.png and print its absolute path."
Each call takes ~1 minute (use a Bash timeout of at least 400000 ms). Create the folder first. Generate 3 variants (different expressions/poses), look at each PNG with the Read tool, and pick the best. If a result is poor, refine the prompt and regenerate (max ~6 generations total).

DELIVERABLE (HTML mockup rule: mockups are plain HTML files in ~/dev/corn/mockups/, never hosted artifacts): write ~/dev/corn/mockups/logos/corny.html, designed at mobile width (max-width ~420px, centered), self-contained except Google Fonts (Bricolage Grotesque for display, Figtree for UI). It must show, using the PNGs by relative path (logos/corny/...):
  1. Concept name + a 2–3 sentence rationale.
  2. The chosen logo large.
  3. It as an Android app icon (rounded squircle) on a dark and a light background.
  4. A fake Android home-screen grid (6–8 neutral placeholder app icons drawn with CSS, generic names like "Clock", "Notes") with the Corn icon among them, labelled "Corn".
  5. Small sizes side by side: 192, 96, 48 px.
  6. A lockup: icon + the wordmark "corn" in Bricolage Grotesque.
  7. The other variants below as "alternates".
No adult content, no real brand logos. Don't touch any other files, don't commit to git.

When done, reply with: the HTML path, the PNG paths, which variant you chose and why (2 sentences), and any problems.
```
