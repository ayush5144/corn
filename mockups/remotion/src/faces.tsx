import React from "react";
import { P } from "./palette";
import { BurstLines, Kernel, Puff, Sparkle, kernelPath } from "./lib";

// The Pop art ("faces") that sit on the button dome. 100x100 grid, centred near (50, 50).
// tone "color": full Corn palette, for the dark dome or a dark background.
// tone "duo":   ink + cream + deep corn, for a corn-yellow surface (yellow art vanishes on yellow).

export type Tone = "color" | "duo";

// 1. Burst: a kernel caught mid-pop, the puff blooming out behind it, burst lines.
// puffK / linesK (0..1) grow the puff and the burst lines; used by the PopAnim animation.
export const BurstFace: React.FC<{ tone: Tone; lines?: boolean; puffK?: number; linesK?: number }> = ({
  tone, lines = true, puffK = 1, linesK = 1,
}) => {
  const duo = tone === "duo";
  const lobes: [number, number, number][] = ([
    [50, 22, 12.5], [35, 28, 10.5], [64, 26, 11.5], [25, 41, 10], [75, 39, 11], [22, 55, 8.5], [78, 54, 9.5], [30, 67, 7], [70, 68, 7.5],
  ] as [number, number, number][]).map(([x, y, r], i) => {
    const k = Math.max(0.001, puffK * (1 + i * 0.03) - i * 0.03); // later lobes lag, all reach 1
    return [50 + (x - 50) * k, 52 + (y - 52) * k, r * k];
  });
  const L: [number, number, number, number][] = [[17, 14, 23, 21], [83, 14, 77, 21], [7, 33, 14, 35.5], [93, 33, 86, 35.5], [50, 1.5, 50, 7.5]];
  const lk = 0.55 + 0.45 * linesK;
  return (
    <g>
      {lines && linesK > 0 && (
        <g opacity={Math.min(1, linesK * 1.5)}>
          <BurstLines color={duo ? P.ink : P.corn} w={4.4}
            lines={L.map(([a, b, c, d]) => [50 + (a - 50) * lk, 45 + (b - 45) * lk, 50 + (c - 50) * lk, 45 + (d - 45) * lk] as [number, number, number, number])} />
        </g>
      )}
      {puffK > 0.02 && <Puff lobes={lobes} outline={duo ? P.ink : undefined} ow={2.2} />}
      <g>
        {duo && <path d={kernelPath(50, 56, 64)} fill={P.ink} transform="translate(50 56) scale(1.08) translate(-50 -56)" />}
        <path d={kernelPath(50, 56, 64)} fill={duo ? "url(#gInk)" : "url(#gKernel)"} filter={duo ? "url(#bvInk)" : "url(#bvCorn)"} />
        {duo ? (
          <path d="M 38 40 q 4 -7 12 -7" stroke={P.cornDeep} strokeWidth={3.2} strokeLinecap="round" fill="none" />
        ) : (
          <>
            <path d="M 38 40 q 4 -7 12 -7" stroke="#FFF8DC" strokeWidth={3.2} strokeLinecap="round" fill="none" opacity={0.9} />
          </>
        )}
      </g>
    </g>
  );
};

// 2. Puff: a big popcorn cloud erupting off the button (after pop-button.png).
export const PuffFace: React.FC<{ tone: Tone; lines?: boolean }> = ({ tone, lines = true }) => {
  const duo = tone === "duo";
  const lobes: [number, number, number][] = [
    [41, 32, 15], [61, 28, 14], [74, 45, 12.5], [27, 49, 12.5], [66, 62, 12], [37, 64, 12], [51, 50, 17],
  ];
  return (
    <g>
      {lines && (
        <BurstLines color={duo ? P.ink : P.corn} w={4.6}
          lines={[[14, 20, 21, 26], [86, 20, 79, 26], [5, 42, 13, 43], [95, 42, 87, 43], [50, 2, 50, 8.5]]} />
      )}
      <Puff lobes={lobes} outline={duo ? P.ink : undefined} ow={2.4} />
    </g>
  );
};

// 3. Cob: the Tab Cob, its top kernel popping off as popcorn.
export const CobFace: React.FC<{ tone: Tone; chunky?: boolean }> = ({ tone, chunky = false }) => {
  const duo = tone === "duo";
  const cols = chunky ? 2 : 4;
  const rows = chunky ? 4 : 7;
  const kw = chunky ? 11 : 6.4;
  const kh = chunky ? 10 : 6.2;
  const gap = chunky ? 1.6 : 1.2;
  const w = cols * kw + (cols + 1) * gap;
  const h = rows * kh + (rows + 1) * gap;
  const angle = chunky ? 22 : 40;
  // cob centred at (44, 56) in its own frame, rotated
  const ox = 44 - w / 2, oy = 58 - h / 2;
  const kernels: React.ReactNode[] = [];
  for (let r = 0; r < rows; r++) {
    for (let c = 0; c < cols; c++) {
      // round off the cob's tip: skip the corner kernels of the top row
      if (!chunky && r === 0 && (c === 0 || c === cols - 1)) continue;
      kernels.push(
        <rect key={`${r}-${c}`} x={ox + gap + c * (kw + gap)} y={oy + gap + r * (kh + gap)} width={kw} height={kh}
          rx={kw * 0.32} fill="url(#gKernel)" filter="url(#bvSmall)" />,
      );
    }
  }
  const lobes: [number, number, number][] = chunky
    ? [[63, 20, 11], [52, 24, 10], [74, 25, 10], [57, 33, 10], [70, 34, 9.5]]
    : [[72, 22, 8.5], [64, 25, 7], [80, 26, 7], [68, 32, 7.5], [77, 33, 6.5]];
  const leafA = chunky
    ? "M 46 92 C 22 84, 16 64, 22 44 C 32 58, 44 70, 50 86 Z"
    : "M 18 88 C 12 70, 20 55, 34 48 C 34 62, 30 76, 22 86 Z";
  const leafB = chunky
    ? "M 44 92 C 64 88, 80 74, 84 58 C 68 64, 54 74, 44 92 Z"
    : "M 18 88 C 36 88, 52 82, 64 70 C 48 72, 32 78, 18 88 Z";
  return (
    <g>
      {/* trail from cob to puff */}
      {!chunky && (
        <g fill={duo ? P.ink : P.cream} opacity={0.85}>
          <circle cx={59} cy={36} r={1.5} /><circle cx={62} cy={33} r={1.1} />
        </g>
      )}
      <BurstLines color={duo ? P.ink : P.corn} w={chunky ? 3.6 : 2.8}
        lines={chunky ? [[40, 10, 44, 15], [85, 9, 81, 15], [91, 27, 86, 28]] : [[72, 7, 72, 11.5], [85, 12, 82.5, 15.5], [90, 25, 86, 26]]} />
      <g transform={`rotate(${angle} 44 58)`}>
        {/* cob body: ink core behind the kernels */}
        <rect x={ox - 1.2} y={oy - 1.2} width={w + 2.4} height={h + 2.4} rx={w * 0.48} fill={duo ? P.ink : "#8A5A00"} />
        {kernels}
      </g>
      <Puff lobes={lobes} outline={duo ? P.ink : undefined} ow={1.8} />
      {duo && <g fill={P.ink}><path d={leafA} transform="translate(-1 1)" /><path d={leafB} transform="translate(1 1)" /></g>}
      <path d={leafA} fill="url(#gLeafDark)" filter="url(#bvLeaf)" />
      <path d={leafB} fill="url(#gLeaf)" filter="url(#bvLeaf)" />
      <path d={chunky ? "M 47 88 C 60 80, 70 72, 78 62" : "M 22 86 C 34 80, 46 76, 58 72"} stroke="#3E6E18" strokeWidth={1}
        fill="none" opacity={0.6} strokeLinecap="round" />
    </g>
  );
};

// 4. Popped kernel: one finished piece of popcorn, hull at the bottom, a sparkle.
export const KernelFace: React.FC<{ tone: Tone; sparkle?: boolean }> = ({ tone, sparkle = true }) => {
  const duo = tone === "duo";
  const lobes: [number, number, number][] = [[38, 36, 16], [60, 34, 16], [30, 56, 16], [68, 56, 16], [49, 50, 16]];
  return (
    <g>
      {sparkle && <Sparkle cx={80} cy={22} r={11} fill={duo ? P.ink : P.corn} />}
      {sparkle && <Sparkle cx={88} cy={40} r={4.5} fill={duo ? P.ink : P.cornLight} />}
      {/* hull */}
      <g transform="rotate(-18 46 72)">
        {duo && <path d="M 34 66 Q 46 92 58 66 Z" fill={P.ink} transform="translate(46 72) scale(1.18) translate(-46 -72)" />}
        <path d="M 34 66 Q 46 92 58 66 Z" fill={duo ? P.cornDeep : "url(#gKernel)"} filter="url(#bvCorn)" />
        <path d="M 46 70 L 46 84" stroke={duo ? P.ink : "#C88400"} strokeWidth={1.2} strokeLinecap="round" opacity={0.6} />
      </g>
      <Puff lobes={lobes} outline={duo ? P.ink : undefined} ow={2.2} />
    </g>
  );
};

export const FACES = { burst: BurstFace, puff: PuffFace, cob: CobFace, kernel: KernelFace };
export { Kernel };
