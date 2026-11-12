// Contact sheet for reviewing renders: each image large on dark, then at 48/24 px on light and dark.
// Usage: node sheet.mjs <out.png> <cols> <size> id1 id2 ...
import { execFileSync } from "node:child_process";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";

const [out, cols, size, ...ids] = process.argv.slice(2);
const dir = path.resolve("out");
const src = (id) => `file://${dir}/${id}.png`;
const html = `<html><body style="margin:0;background:#0F0E0B;font:12px sans-serif;color:#A79F8A">
<div style="display:grid;grid-template-columns:repeat(${cols},${size}px)">
${ids.map((id) => `<div style="height:${size}px;position:relative"><img src="${src(id)}" style="width:100%;height:100%"><span style="position:absolute;left:6px;top:4px">${id}</span></div>`).join("")}
${ids.map((id) => `<div style="height:70px;display:flex;gap:10px;align-items:center;justify-content:center;background:#FBF8F1">
<img src="${src(id)}" width=48><img src="${src(id)}" width=24>
<span style="background:#0F0E0B;padding:8px;display:flex;gap:8px"><img src="${src(id)}" width=24><span style="background:#FFC83D;width:46px;height:40px;border-radius:12px;display:grid;place-items:center"><img src="${src(id)}" width=36></span></span></div>`).join("")}
</div></body></html>`;
const tmp = path.join(os.tmpdir(), `sheet-${process.pid}.html`);
fs.writeFileSync(tmp, html);
const rows = Math.ceil(ids.length / Number(cols));
execFileSync("/Applications/Google Chrome.app/Contents/MacOS/Google Chrome", [
  "--headless=new", "--disable-gpu", "--hide-scrollbars", "--allow-file-access-from-files",
  `--window-size=${Number(cols) * Number(size)},${rows * (Number(size) + 70)}`, `--screenshot=${path.resolve(out)}`, `file://${tmp}`,
], { stdio: "ignore" });
fs.unlinkSync(tmp);
