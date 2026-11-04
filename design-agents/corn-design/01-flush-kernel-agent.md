# Agent: Corn logo concept 1, "The Flush Kernel"

- **Run on:** 2026-11-05, as a Claude Code subagent (`general-purpose`), in the background, in parallel with concepts 2 and 3.
- **Tools it used:** Bash (Codex CLI image generation, `sips`, headless Chrome), Read (to look at each PNG), Write.
- **Output:** `mockups/logos/flush-kernel.html` and `mockups/logos/flush-kernel/*.png` (chosen: `v5-tooth-vortex.png`). Full-size originals: `~/Documents/corn-logo-originals/logos/flush-kernel/`.
- **Result:** 5 generations; chose "Tooth vortex" (the only one that read clearly as a corn kernel).

## Exact brief given to the agent

```text
You are designing ONE logo concept for "Corn", a free, private, mobile-first Android browser for adults whose personality is "cool, funny, modern". Its signature feature is the **Flush Button**: one tap wipes all browsing (tabs, cookies, history) like a toilet flush. The current logo (~/dev/corn/brand/cornbrowser-logo-square-transparent-1024.png) is a glossy realistic corn cob; the founder says it's "too real". Do NOT modify anything in ~/dev/corn/brand/.

YOUR CONCEPT: "The Flush Kernel" — a single, bold, geometric corn kernel (not a whole cob) whose negative space forms a swirl / drain vortex, so the mark reads as both "corn" and "flush". Minimal, flat, iconic, works at 48px, no gradients-heavy realism, no text inside the icon. Palette: corn yellow #FFC83D, dark ink #2A1E00, near-black background #0F0E0B, optional leaf green #86C64A. Surprise us — be creative within this direction.

HOW TO GENERATE IMAGES (tested, works): run Codex's image generation from the shell, e.g.
  cd ~/dev/corn/mockups/logos/flush-kernel && codex exec --skip-git-repo-check -s workspace-write "Use your image generation tool to create <detailed prompt>. 1024x1024 PNG. Save it in the current directory as <name>.png and print its absolute path."
Each call takes ~1 minute (use a Bash timeout of at least 400000 ms). Create the folder first. Generate 3 variants (different takes within the concept), look at each PNG with the Read tool, and pick the best. Ask for flat vector-style app-icon artwork. If a result is poor, refine the prompt and regenerate (max ~6 generations total).

DELIVERABLE (HTML mockup rule: mockups are plain HTML files in ~/dev/corn/mockups/, never hosted artifacts): write ~/dev/corn/mockups/logos/flush-kernel.html, designed at mobile width (max-width ~420px, centered), self-contained except Google Fonts (Bricolage Grotesque for display, Figtree for UI). It must show, using the PNGs by relative path (logos/flush-kernel/...):
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
