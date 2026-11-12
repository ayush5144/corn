import React from "react";
import { AbsoluteFill } from "remotion";
import { P } from "./palette";

// "Burst": a corn kernel caught mid-pop, a cream puff bursting out behind it, short burst lines.
// Reference: mockups/icons/pop-burst.png (AI concept). Drawn on a 100x100 grid.
export const PopBurst: React.FC<{ bg?: string }> = ({ bg = P.bg }) => {
  // Popcorn puff: overlapping cream lobes behind and above the kernel.
  const puffs: [number, number, number][] = [
    [50, 30, 14], [35, 35, 12], [65, 35, 12], [27, 49, 10.5], [73, 49, 10.5], [38, 23, 9], [62, 23, 9],
  ];
  // Burst lines, kept clear of the puff.
  const lines: [number, number, number, number][] = [
    [20, 15, 25, 21], [80, 15, 75, 21], [11, 34, 17, 36], [89, 34, 83, 36], [50, 5, 50, 10],
  ];
  return (
    <AbsoluteFill style={{ backgroundColor: bg }}>
      <svg viewBox="0 0 100 100" width="100%" height="100%">
        <defs>
          <radialGradient id="puff" cx="40%" cy="35%" r="70%">
            <stop offset="0%" stopColor="#FFFFFF" />
            <stop offset="60%" stopColor={P.cream} />
            <stop offset="100%" stopColor={P.creamShade} />
          </radialGradient>
          <linearGradient id="kernel" x1="0" y1="0" x2="1" y2="1">
            <stop offset="0%" stopColor={P.cornLight} />
            <stop offset="45%" stopColor={P.corn} />
            <stop offset="100%" stopColor={P.cornDeep} />
          </linearGradient>
        </defs>
        {lines.map(([x1, y1, x2, y2], i) => (
          <line key={i} x1={x1} y1={y1} x2={x2} y2={y2} stroke={P.corn} strokeWidth={3.4} strokeLinecap="round" />
        ))}
        {puffs.map(([cx, cy, r], i) => <circle key={i} cx={cx} cy={cy} r={r} fill="url(#puff)" />)}
        {/* the kernel: a corn kernel's shape, wide rounded crown tapering to a tip */}
        <path d="M50 40 C 64 40, 70 48, 68 58 C 66 70, 57 82, 50 88 C 43 82, 34 70, 32 58 C 30 48, 36 40, 50 40 Z"
          fill="url(#kernel)" stroke={P.cornDeep} strokeWidth={1.4} strokeLinejoin="round" />
        {/* the crown's soft dent and a highlight */}
        <path d="M41 47 C 46 44, 54 44, 59 47" stroke={P.cornDeep} strokeWidth={1.4} strokeLinecap="round" fill="none" opacity={0.6} />
        <path d="M39 54 C 39 62, 42 70, 46 76" stroke="#FFF3C8" strokeWidth={2.6} strokeLinecap="round" fill="none" opacity={0.85} />
      </svg>
    </AbsoluteFill>
  );
};
