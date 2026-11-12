import React from "react";
import { Puff } from "../lib";
import { ArtProps, Blush, Eye, INK, Mouth, O, Popcorn, Shine, Strokes, Wink, fatKernel } from "./shared";

// 1. Kernel Pop: a plump kernel with a nervous little face, wobbling on the button.
// Tap: it wobbles harder and bursts into a fat piece of popcorn that winks at you.
export const KernelPopArt: React.FC<ArtProps> = ({ pose }) => {
  if (pose === "rest") {
    return (
      <g>
        {/* wobble marks */}
        <Strokes d="M 20 32 q -4.5 8 0 16 M 80 32 q 4.5 8 0 16" w={2.8} />
        <Strokes d="M 13 36 q -2.5 4 0 8 M 87 36 q 2.5 4 0 8" w={2.2} />
        <g transform="rotate(-6 50 64)">
          <O d={fatKernel(50, 38, 54)} fill="url(#gKernel)" filter="url(#bvCorn)" ow={2.6} />
          {/* the pale germ at the tip, where it'll split */}
          <path d="M 50 47 C 56 47 57.5 54 54 60 Q 50 64 46 60 C 42.5 54 44 47 50 47 Z" fill="#FFEDB5" opacity={0.7} />
          {/* a hairline crack in the crown */}
          <Strokes color={INK} d="M 52 14 l -1.5 3.6 l 2.6 2.6 l -1.8 3.4" w={1.5} />
          <Shine d="M 34 22 q 3 -7 10 -8.5" w={3.2} />
          <Eye x={42.5} y={33} r={3.3} look={[1.2, -0.7]} />
          <Eye x={58} y={33} r={3.3} look={[1.2, -0.7]} />
          {/* nervous wavy mouth */}
          <Mouth d="M 43.5 43 q 2.2 -2.2 4.4 0 t 4.4 0 t 4.4 0" w={2} />
          {/* sweat drop */}
          <O d="M 70 20 q 3.8 5.4 0 8 q -3.8 -2.6 0 -8 Z" fill="#DFF3FF" ow={1.3} />
        </g>
      </g>
    );
  }
  // action: popped
  const lobes: [number, number, number][] = [
    [38, 28, 12.5], [60, 25, 13], [72, 40, 11], [27, 43, 11], [35, 55, 10.5], [64, 55, 11], [50, 42, 15.5],
  ];
  return (
    <g>
      <Strokes d="M 16 14 l 6 6 M 84 12 l -6 6 M 6 36 h 8 M 94 34 h -8 M 50 1 v 7" w={3} />
      {/* hull fragments flying off */}
      <O d="M 10 24 q 3 -3 6 0 q -3 4 -6 0 Z" fill="url(#gKernel)" ow={1.3} />
      <O d="M 85 22 q 3 -2 5 1 q -3 3 -5 -1 Z" fill="url(#gKernel)" ow={1.3} />
      {/* the split hull, under the puff */}
      <O d="M 37 56 Q 50 78 63 56 Z" fill="url(#gKernel)" filter="url(#bvCorn)" ow={2.4} />
      <Puff lobes={lobes} outline={INK} ow={2.6} />
      {/* the face */}
      <Wink x={42} y={39} r={4} w={2.3} />
      <Eye x={58} y={38} r={3.4} />
      <Mouth d="M 43 47 Q 50 57 58 47 Q 50 50 43 47 Z" w={2} fill={INK} />
      <path d="M 47 51.4 Q 50.5 54 54 51.4" stroke="#FF8A5C" strokeWidth={2} strokeLinecap="round" fill="none" />
      <Blush x={35} y={46} /><Blush x={66} y={45} />
      <Popcorn x={89} y={50} s={0.7} rot={20} />
      <Popcorn x={11} y={56} s={0.55} rot={-25} />
    </g>
  );
};
