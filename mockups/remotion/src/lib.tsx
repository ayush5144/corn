import React from "react";
import { AbsoluteFill } from "remotion";
import { P } from "./palette";

// Shared drawing kit for the Corn icon concepts. Everything is drawn on a 100x100 grid,
// light from the top-left. Rendered with a transparent background unless `bg` is given,
// so the same PNG can sit on the dark and the light toolbar.

export type Variant = "a" | "b" | "c";
export type Use = "vault" | "save" | "saved";

export const Canvas: React.FC<{ bg?: string; children: React.ReactNode; defs?: React.ReactNode; viewBox?: string }> = ({
  bg = "transparent", children, defs, viewBox = "0 0 100 100",
}) => (
  <AbsoluteFill style={{ backgroundColor: bg }}>
    <svg viewBox={viewBox} width="100%" height="100%">
      <defs>
        <CommonDefs />
        {defs}
      </defs>
      {children}
    </svg>
  </AbsoluteFill>
);

// Bevel: an inner shadow on the bottom-right edge and a rim light on the top-left edge,
// both clipped to the shape. This is what gives flat shapes their soft "app icon" depth.
export const Bevel: React.FC<{ id: string; shade: string; light?: string; d?: number; blur?: number; so?: number; lo?: number }> = ({
  id, shade, light = "#FFFFFF", d = 1.6, blur = 1.2, so = 0.55, lo = 0.6,
}) => (
  <filter id={id} x="-20%" y="-20%" width="140%" height="140%" filterUnits="userSpaceOnUse">
    {/* shade: shape minus itself shifted up-left -> bottom-right rim */}
    <feOffset in="SourceAlpha" dx={-d} dy={-d} result="o1" />
    <feGaussianBlur in="o1" stdDeviation={blur} result="b1" />
    <feComposite in="SourceAlpha" in2="b1" operator="out" result="rim1" />
    <feFlood floodColor={shade} floodOpacity={so} />
    <feComposite in2="rim1" operator="in" result="shadeRim" />
    {/* light: shape minus itself shifted down-right -> top-left rim */}
    <feOffset in="SourceAlpha" dx={d * 0.7} dy={d * 0.7} result="o2" />
    <feGaussianBlur in="o2" stdDeviation={blur * 0.8} result="b2" />
    <feComposite in="SourceAlpha" in2="b2" operator="out" result="rim2" />
    <feFlood floodColor={light} floodOpacity={lo} />
    <feComposite in2="rim2" operator="in" result="lightRim" />
    <feMerge>
      <feMergeNode in="SourceGraphic" />
      <feMergeNode in="shadeRim" />
      <feMergeNode in="lightRim" />
    </feMerge>
  </filter>
);

const CommonDefs: React.FC = () => (
  <>
    <filter id="drop" x="-30%" y="-30%" width="160%" height="160%" filterUnits="userSpaceOnUse">
      <feGaussianBlur in="SourceAlpha" stdDeviation="1.6" />
      <feOffset dx="0.8" dy="1.8" result="b" />
      <feFlood floodColor="#000" floodOpacity="0.38" />
      <feComposite in2="b" operator="in" />
      <feMerge><feMergeNode /><feMergeNode in="SourceGraphic" /></feMerge>
    </filter>
    <filter id="soft" x="-50%" y="-50%" width="200%" height="200%">
      <feGaussianBlur stdDeviation="2.4" />
    </filter>
    {/* faint paper grain, used sparingly on big surfaces */}
    <filter id="grain" x="0" y="0" width="100%" height="100%">
      <feTurbulence type="fractalNoise" baseFrequency="2.2" numOctaves="2" seed="7" result="n" />
      <feColorMatrix in="n" type="matrix" values="0 0 0 0 0  0 0 0 0 0  0 0 0 0 0  0 0 0 0.09 0" result="g" />
      <feComposite in="g" in2="SourceAlpha" operator="in" result="gi" />
      <feMerge><feMergeNode in="SourceGraphic" /><feMergeNode in="gi" /></feMerge>
    </filter>
    <Bevel id="bvCorn" shade="#B26F00" light="#FFF4C4" />
    <Bevel id="bvCream" shade="#B8A57C" light="#FFFFFF" so={0.6} lo={0.9} />
    <Bevel id="bvLeaf" shade="#2F5A12" light="#D8F5A8" so={0.5} lo={0.5} />
    <Bevel id="bvInk" shade="#000000" light="#6B5A2E" so={0.5} lo={0.55} />
    <Bevel id="bvSmall" shade="#A86400" light="#FFF6D0" d={0.9} blur={0.6} so={0.5} lo={0.7} />

    <radialGradient id="gCream" cx="35%" cy="30%" r="80%">
      <stop offset="0%" stopColor="#FFFFFF" />
      <stop offset="55%" stopColor={P.cream} />
      <stop offset="100%" stopColor={P.creamShade} />
    </radialGradient>
    <radialGradient id="gCorn" cx="32%" cy="28%" r="85%">
      <stop offset="0%" stopColor={P.cornLight} />
      <stop offset="45%" stopColor={P.corn} />
      <stop offset="100%" stopColor={P.cornDeep} />
    </radialGradient>
    <radialGradient id="gKernel" cx="35%" cy="28%" r="80%">
      <stop offset="0%" stopColor="#FFE9A6" />
      <stop offset="50%" stopColor={P.corn} />
      <stop offset="100%" stopColor="#E09A00" />
    </radialGradient>
    <linearGradient id="gLeaf" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0%" stopColor="#A6DC6A" />
      <stop offset="55%" stopColor={P.leaf} />
      <stop offset="100%" stopColor={P.leafDeep} />
    </linearGradient>
    <linearGradient id="gLeafDark" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0%" stopColor="#77B23F" />
      <stop offset="100%" stopColor="#4A7E20" />
    </linearGradient>
    <radialGradient id="gInk" cx="35%" cy="25%" r="90%">
      <stop offset="0%" stopColor="#4A3D22" />
      <stop offset="60%" stopColor={P.ink} />
      <stop offset="100%" stopColor="#140E00" />
    </radialGradient>
    <radialGradient id="gNight" cx="35%" cy="25%" r="90%">
      <stop offset="0%" stopColor="#3D382D" />
      <stop offset="60%" stopColor={P.surface} />
      <stop offset="100%" stopColor="#15130E" />
    </radialGradient>
  </>
);

// ---------- Corn motifs ----------

// A popcorn puff: one cloud made of overlapping lobes. A single gradient spans the whole cloud
// (lit from the top-left), the cloud is bevelled as one shape, and faint creases mark the lobes.
let puffN = 0;
export const Puff: React.FC<{
  lobes: [number, number, number][]; filter?: string; outline?: string; ow?: number; base?: string; shade?: string;
}> = ({ lobes, filter = "url(#bvCream)", outline, ow = 2, base = P.cream, shade = P.creamShade }) => {
  const id = React.useMemo(() => `puff${puffN++}`, []);
  const x0 = Math.min(...lobes.map(([x, , r]) => x - r)), x1 = Math.max(...lobes.map(([x, , r]) => x + r));
  const y0 = Math.min(...lobes.map(([, y, r]) => y - r)), y1 = Math.max(...lobes.map(([, y, r]) => y + r));
  const w = x1 - x0, h = y1 - y0;
  return (
    <g>
      <defs>
        <radialGradient id={id} gradientUnits="userSpaceOnUse" cx={x0 + w * 0.32} cy={y0 + h * 0.25} r={Math.max(w, h) * 0.95}>
          <stop offset="0%" stopColor="#FFFFFF" />
          <stop offset="45%" stopColor={base} />
          <stop offset="100%" stopColor={shade} />
        </radialGradient>
      </defs>
      {outline && (
        <g fill={outline}>{lobes.map(([x, y, r], i) => <circle key={i} cx={x} cy={y} r={r + ow} />)}</g>
      )}
      <g filter={filter} fill={`url(#${id})`}>
        {lobes.map(([x, y, r], i) => <circle key={i} cx={x} cy={y} r={r} />)}
      </g>
      {/* creases: the lower-right edge of each front lobe, faint */}
      <g fill="none" stroke="#B9A57A" strokeLinecap="round" opacity={0.45}>
        {lobes.slice(1).map(([x, y, r], i) => (
          <path key={i} strokeWidth={r * 0.07}
            d={`M ${x + r * 0.97} ${y - r * 0.26} A ${r} ${r} 0 0 1 ${x + r * 0.26} ${y + r * 0.97}`} />
        ))}
      </g>
    </g>
  );
};

// A corn kernel: rounded crown tapering to a tip. Centred at (cx, cy), size s (height).
export const kernelPath = (cx: number, cy: number, s: number) => {
  const w = s * 0.6, top = cy - s / 2, bot = cy + s / 2;
  return `M ${cx} ${top} C ${cx + w * 0.56} ${top}, ${cx + w * 0.54} ${top + s * 0.3}, ${cx + w * 0.47} ${cy + s * 0.12}
    C ${cx + w * 0.4} ${cy + s * 0.36}, ${cx + w * 0.24} ${bot}, ${cx} ${bot}
    C ${cx - w * 0.24} ${bot}, ${cx - w * 0.4} ${cy + s * 0.36}, ${cx - w * 0.47} ${cy + s * 0.12}
    C ${cx - w * 0.54} ${top + s * 0.3}, ${cx - w * 0.56} ${top}, ${cx} ${top} Z`;
};

export const Kernel: React.FC<{ cx: number; cy: number; s: number; fill?: string; filter?: string; shine?: boolean; rot?: number }> = ({
  cx, cy, s, fill = "url(#gKernel)", filter = "url(#bvCorn)", shine = true, rot = 0,
}) => (
  <g transform={`rotate(${rot} ${cx} ${cy})`}>
    <path d={kernelPath(cx, cy, s)} fill={fill} filter={filter} />
    {shine && (
      <path d={`M ${cx - s * 0.2} ${cy - s * 0.28} q ${s * 0.12} ${-s * 0.1} ${s * 0.26} ${-s * 0.07}`}
        stroke="#FFFBEA" strokeWidth={s * 0.07} strokeLinecap="round" fill="none" opacity={0.85} />
    )}
  </g>
);

export const BurstLines: React.FC<{ lines: [number, number, number, number][]; color?: string; w?: number }> = ({
  lines, color = P.corn, w = 3.2,
}) => (
  <g stroke={color} strokeWidth={w} strokeLinecap="round">
    {lines.map(([x1, y1, x2, y2], i) => <line key={i} x1={x1} y1={y1} x2={x2} y2={y2} />)}
  </g>
);

export const Sparkle: React.FC<{ cx: number; cy: number; r: number; fill?: string }> = ({ cx, cy, r, fill = P.corn }) => {
  const k = r * 0.22;
  return (
    <path fill={fill} d={`M ${cx} ${cy - r} Q ${cx + k} ${cy - k} ${cx + r} ${cy} Q ${cx + k} ${cy + k} ${cx} ${cy + r}
      Q ${cx - k} ${cy + k} ${cx - r} ${cy} Q ${cx - k} ${cy - k} ${cx} ${cy - r} Z`} />
  );
};

// ---------- Vault uses: + / check badge drawn in code ----------
// The icon is masked so a clean gap surrounds the badge on any background.
export const WithBadge: React.FC<{ use?: Use; children: React.ReactNode; at?: [number, number] }> = ({
  use = "vault", children, at = [78, 78],
}) => {
  if (use === "vault") return <>{children}</>;
  const [x, y] = at;
  const r = 15;
  return (
    <>
      <mask id="badgeCut" maskUnits="userSpaceOnUse" x="0" y="0" width="100" height="100">
        <rect width="100" height="100" fill="#fff" />
        <circle cx={x} cy={y} r={r + 4} fill="#000" />
      </mask>
      <g mask="url(#badgeCut)">{children}</g>
      <g filter="url(#drop)">
        <circle cx={x} cy={y} r={r} fill={use === "save" ? "url(#gCorn)" : "url(#gLeaf)"} />
      </g>
      <circle cx={x} cy={y} r={r - 0.8} fill="none" stroke="#FFFFFF" strokeOpacity={0.45} strokeWidth={1.2}
        strokeDasharray={`${r * 2.2} 200`} transform={`rotate(150 ${x} ${y})`} />
      {use === "save" ? (
        <path d={`M ${x} ${y - 7.5} V ${y + 7.5} M ${x - 7.5} ${y} H ${x + 7.5}`} stroke={P.ink} strokeWidth={4.4} strokeLinecap="round" />
      ) : (
        <path d={`M ${x - 7} ${y + 0.5} L ${x - 2} ${y + 5.5} L ${x + 7.5} ${y - 5}`} stroke={P.ink} strokeWidth={4.4}
          strokeLinecap="round" strokeLinejoin="round" fill="none" />
      )}
    </>
  );
};
