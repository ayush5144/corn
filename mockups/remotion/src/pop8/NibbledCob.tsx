import React from "react";
import { P } from "../palette";
import { ArtProps, INK, O, OKernel, Say, Shine, Strokes } from "./shared";

// 8. Nibbled Cob: a full cob lying on its side like a typewriter carriage, a bell on the end.
// Tap: kernels vanish row by row, left to right, typewriter-style; "ding!" at the end of each
// row, the carriage slides back. A clean cob.
const COB = "M 26 28 H 78 Q 90 28 90 42 Q 90 56 78 56 H 26 Q 14 56 14 42 Q 14 28 26 28 Z";
const ROWS = 4, COLS = 10;

// eaten(r, c): whether the kernel at row r, column c is gone.
export const NibbledCobArt: React.FC<ArtProps & { eaten?: (r: number, c: number) => boolean }> = ({ pose, eaten }) => {
  const isEaten = eaten ?? (pose === "rest" ? () => false : (r: number, c: number) => r < 2 || (r === 2 && c < 6));
  const kernels: React.ReactNode[] = [];
  const pits: React.ReactNode[] = [];
  for (let r = 0; r < ROWS; r++) {
    for (let c = 0; c < COLS; c++) {
      const x = 16 + c * 7.4, y = 28.6 + r * 6.9;
      if (isEaten(r, c)) {
        pits.push(<ellipse key={`p${r}-${c}`} cx={x + 3.4} cy={y + 3.1} rx={2.1} ry={1.7} fill="#C9A86A" opacity={0.75} />);
      } else {
        kernels.push(<rect key={`${r}-${c}`} x={x} y={y} width={6.8} height={6.3} rx={2.6} fill="url(#gKernel)" stroke={INK} strokeWidth={1.2} />);
      }
    }
  }
  const ding = pose === "action";
  return (
    <g transform="translate(0 6)">
      <defs>
        <clipPath id="nibCob"><path d={COB} /></clipPath>
        <linearGradient id="gCyl" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0" stopColor="#FFFFFF" stopOpacity={0.35} />
          <stop offset="0.3" stopColor="#FFFFFF" stopOpacity={0} />
          <stop offset="0.75" stopColor="#7A4A00" stopOpacity={0} />
          <stop offset="1" stopColor="#7A4A00" stopOpacity={0.35} />
        </linearGradient>
        <linearGradient id="gCore" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0" stopColor="#FFF6DC" /><stop offset="1" stopColor="#E9D29C" />
        </linearGradient>
      </defs>
      {/* the bell, on a little post at the right end */}
      <g transform={ding ? "rotate(14 84 22)" : undefined}>
        <path d="M 84 22 V 28" stroke={INK} strokeWidth={2.4} />
        <O d="M 77 22 Q 77 12 84 12 Q 91 12 91 22 Z" fill="url(#gCream)" filter="url(#bvCream)" ow={1.8} />
        <O d="M 75.6 21 H 92.4 V 23.4 H 75.6 Z" fill="url(#gCream)" ow={1.2} />
        <circle cx={84} cy={10.6} r={1.8} fill={INK} />
        <Shine d="M 80 18 q 0 -3 2 -4.4" w={1.4} o={0.9} />
      </g>
      {ding && (
        <>
          <Strokes d="M 94 12 l 4 -3 M 95 18 h 5 M 92 7 l 2 -4" w={2} />
          <Say x={70} y={0} text="ding!" size={8} rot={-6} />
        </>
      )}
      {/* the cob */}
      <O d={COB} fill="url(#gCore)" ow={2.6} />
      <g clipPath="url(#nibCob)">
        {pits}
        {kernels}
        <rect x={14} y={28} width={76} height={28} fill="url(#gCyl)" />
      </g>
      <path d={COB} fill="none" stroke={INK} strokeWidth={2.4} />
      {/* husk tuft on the left end */}
      <O d="M 20 40 C 12 30 6 30 2 24 C 6 36 6 44 16 48 Z" fill="url(#gLeaf)" filter="url(#bvLeaf)" ow={2} />
      <O d="M 20 44 C 10 46 6 54 2 58 C 10 58 16 54 22 50 Z" fill="url(#gLeafDark)" filter="url(#bvLeaf)" ow={2} />
      {/* the carriage lever (return arm) on the left */}
      <path d="M 14 34 L 6 16" stroke={INK} strokeWidth={4.6} strokeLinecap="round" />
      <path d="M 14 34 L 6 16" stroke="#D9D2C2" strokeWidth={2.2} strokeLinecap="round" />
      <circle cx={6} cy={15} r={3} fill={INK} /><circle cx={6} cy={15} r={1.8} fill={P.leaf} />
      {ding && (
        <>
          <OKernel cx={44} cy={18} s={6} rot={-30} ow={1.2} />
          <OKernel cx={56} cy={14} s={5} rot={40} ow={1.2} />
          <OKernel cx={36} cy={12} s={4.4} rot={10} ow={1.1} />
          <Strokes d="M 40 24 l -2 -3 M 60 22 l 2 -3" w={1.6} />
        </>
      )}
    </g>
  );
};
