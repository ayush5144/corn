// Render every <Still> (or those matching a filter) to out/<id>.png in one bundle.
// Usage: node render.mjs [substring-filter]   (e.g. node render.mjs pop-burst)
// Also writes a dark-background contact sheet per filter run when --sheet is passed.
import { bundle } from "@remotion/bundler";
import { getCompositions, renderStill } from "@remotion/renderer";
import path from "node:path";
import fs from "node:fs";

const filter = process.argv[2] && !process.argv[2].startsWith("--") ? process.argv[2] : "";
const t0 = Date.now();
const serveUrl = await bundle({ entryPoint: path.resolve("src/index.ts") });
const comps = (await getCompositions(serveUrl)).filter((c) => c.durationInFrames === 1 && c.id.includes(filter));
fs.mkdirSync("out", { recursive: true });
const times = [];
for (const c of comps) {
  const t = Date.now();
  await renderStill({ composition: c, serveUrl, output: `out/${c.id}.png`, imageFormat: "png", overwrite: true });
  times.push(Date.now() - t);
  console.log(`${c.id}  ${Date.now() - t} ms`);
}
const total = Date.now() - t0;
console.log(`${comps.length} stills in ${(total / 1000).toFixed(1)} s (avg ${Math.round(times.reduce((a, b) => a + b, 0) / Math.max(1, times.length))} ms each, bundling included in total)`);
