# Design agents

The exact AI agents used to explore Corn's logo and a possible rename to "Flush", saved so the work can be repeated or extended.

| Folder | What |
|---|---|
| `corn-design/` | The three Corn logo agents: Flush Kernel, Corny, Tab Cob. Each file has the brief word for word and what came out. |
| `flush-design/` | The Flush brand agent (logos, colour theme, copy kit) and the Flush per-concept pages agent (Vortex, Button, Wink). |
| `../.claude/agents/corn-logo-designer.md`, `flush-brand-designer.md` | Reusable versions of the same agents. Claude Code can call them by name in any session in this repo. |

No skills were used: only Claude Code subagents (`general-purpose`) and Codex's image generation tool.

## How the process ran (2026-11-05)

1. **Test the image tool first.** The main session ran one Codex call to confirm image generation works and where the file is saved:
   ```bash
   codex exec --skip-git-repo-check -s workspace-write "Use your image generation tool to create <prompt>. 1024x1024 PNG. Save it in the current directory as test.png and print its absolute path."
   ```
   About one minute per image. Codex CLI 0.160.0, `image_generation` feature enabled.
2. **Write one brief per agent, each with a different creative direction**, so the options really differ instead of being variations of one idea. Every brief states: the product and its personality, the concept, the palette, the exact Codex command, a generation limit, the HTML page it must produce (mobile width, plain HTML, Google Fonts only), files it must not touch, and what to report back.
3. **Run the agents in parallel, in the background** (about 5 minutes each).
4. **Review every result in the main session**: look at each chosen PNG, screenshot each page with headless Chrome, fix layout problems (Corny's sizes row was too wide for 420 px).
5. **Build the comparison page** (`mockups/brand-compare.html`) by hand.
6. **Shrink PNGs to 512 px in the repo** (`sips -Z 512`); keep full-size originals outside it in `~/Documents/corn-logo-originals/`. 18 PNGs were 16 MB at full size.

## Rerun or extend

Ask Claude Code, for example:
- "Use the corn-logo-designer agent for a new concept: <idea>."
- "Use the flush-brand-designer agent to make two more variants of the Wink concept."

Or paste a brief from these files into a new agent and change the concept.

**Rules the agents follow:** mockups are plain HTML in `mockups/` (never hosted pages); no adult content; no real brand logos; reference images are for mood only (never traced or passed to the image model); don't touch `brand/` or app code; don't commit.
