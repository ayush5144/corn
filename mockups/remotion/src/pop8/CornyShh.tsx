import React from "react";
import { P } from "../palette";
import { ArtProps, Blush, Eye, INK, Mouth, O, Popcorn, Say, Shine, Strokes, Wink } from "./shared";

// 3. Corny Shh: a tiny Corny (the logo) is the button, looking innocent.
// Tap: he winks, raises a "shh" finger to his lips, and the popcorn goes off behind him.
const BODY = "M 50 7 C 66 7 72 22 72 36 C 72 52 64 64 50 64 C 36 64 28 52 28 36 C 28 22 34 7 50 7 Z";

const KernelRows: React.FC = () => {
  const rows: React.ReactNode[] = [];
  const spec: [number, number, number][] = [[6, 6.4, 0], [12.8, 7.2, 4], [20.6, 7.6, 0]]; // y, h, x offset
  spec.forEach(([y, h, off], r) => {
    for (let x = 22 + off; x < 78; x += 8.4) {
      rows.push(<rect key={`${r}-${x}`} x={x} y={y} width={7.6} height={h} rx={3} fill="url(#gKernel)" stroke={INK} strokeWidth={1.3} />);
    }
  });
  // bottom rows peeking above the leaves
  for (let x = 30; x < 70; x += 8.4) {
    rows.push(<rect key={`b-${x}`} x={x} y={56} width={7.6} height={7} rx={3} fill="url(#gKernel)" stroke={INK} strokeWidth={1.3} />);
  }
  return <g>{rows}</g>;
};

export const CornyShhArt: React.FC<ArtProps> = ({ pose }) => {
  const shh = pose === "action";
  return (
    <g>
      {shh && (
        <>
          <Strokes d="M 14 18 l 6 5 M 86 16 l -6 5 M 50 -3 v 5" w={2.8} />
          <Popcorn x={17} y={30} s={0.85} rot={-20} />
          <Popcorn x={84} y={34} s={0.75} rot={18} />
          <Popcorn x={27} y={6} s={0.55} rot={10} />
        </>
      )}
      <defs><clipPath id="cornyBody"><path d={BODY} /></clipPath></defs>
      <O d={BODY} fill="url(#gCorn)" filter="url(#bvCorn)" ow={2.6} />
      <g clipPath="url(#cornyBody)"><KernelRows /></g>
      <path d={BODY} fill="none" stroke={INK} strokeWidth={2.4} />
      <Shine d="M 33 33 q 0 -6 2 -9" w={2.4} o={0.7} />
      {shh ? (
        <>
          <Wink x={41.5} y={37.5} r={3.6} w={2.2} />
          {/* the half-lidded eye, like the logo */}
          <Eye x={58.5} y={38.6} r={2.7} look={[0.6, 0.4]} />
          <path d="M 53.5 35.6 H 64" stroke={INK} strokeWidth={2.2} strokeLinecap="round" />
          <Mouth d="M 43 45.5 Q 46 47.5 47 47.4 M 51.8 47.2 Q 55 47 57.5 44.5" w={2} />
          {/* the shh finger, straight up across the lips; the fist curls off to the right */}
          <O d="M 52 51 C 52 47 51 45 55 44.5 C 59 44 61 46.5 61 50 C 61 55 58 58 54 58 C 50.5 58 49 56 49 54 Z" fill="url(#gCorn)" ow={1.5} />
          <path d="M 53.5 48.6 Q 57 47.6 59.4 49.4 M 53.2 52 Q 57 51.2 59.8 53 M 54.4 55.4 Q 57 55 58.6 56" stroke={INK} strokeWidth={1.1} strokeLinecap="round" fill="none" opacity={0.8} />
          <O d="M 47 38.5 Q 47 35.6 49.4 35.6 Q 51.8 35.6 51.8 38.5 V 54 Q 51.8 56.4 49.4 56.4 Q 47 56.4 47 54 Z" fill="url(#gCorn)" ow={1.6} />
          <path d="M 48.2 38.2 Q 49.4 37.2 50.6 38.2" stroke={INK} strokeWidth={0.9} fill="none" strokeLinecap="round" opacity={0.7} />
          <Say x={77} y={14} text="shh" size={8.5} rot={8} />
        </>
      ) : (
        <>
          <Eye x={42} y={37.5} r={3.2} look={[1, 0]} />
          <Eye x={58} y={37.5} r={3.2} look={[1, 0]} />
          <Mouth d="M 43.5 45 Q 50 50.5 56.5 45" w={2.1} />
          <Blush x={36} y={44} r={2.4} /><Blush x={64} y={44} r={2.4} />
        </>
      )}
      {/* husk leaves */}
      <O d="M 46 66 C 34 66 22 58 20 38 C 28 44 36 48 40 56 Z" fill="url(#gLeaf)" filter="url(#bvLeaf)" ow={2.2} />
      <O d="M 54 66 C 66 66 78 58 80 38 C 72 44 64 48 60 56 Z" fill="url(#gLeaf)" filter="url(#bvLeaf)" ow={2.2} />
      <path d="M 25 44 C 30 52 36 58 42 62 M 75 44 C 70 52 64 58 58 62" stroke={P.leafDeep} strokeWidth={1.2}
        strokeLinecap="round" fill="none" />
    </g>
  );
};
