import React from "react";
import { P } from "../palette";
import { ArtProps, Eye, INK, O, Popcorn, Shine, Strokes } from "./shared";

// 6. Kernel Rocket: a kernel (tip up, for a nose) with leaf fins and a porthole,
// someone peeking out of it. Tap: it blasts off, leaving a trail of popcorn puffs. Gone.
// a kernel upside down: the pointy tip is the nose, the dented crown is the base
const BODY = "M 50 4 C 56 4 66 22 68 38 C 69.5 48 66 55 60 56.5 Q 55 55 50 57 Q 45 55 40 56.5 C 34 55 30.5 48 32 38 C 34 22 44 4 50 4 Z";
const Rocket: React.FC<{ flame: boolean }> = ({ flame }) => (
  <g>
    {flame && (
      <g>
        <O d="M 42 58 Q 50 92 58 58 Z" fill="#FF8A3D" ow={1.8} />
        <path d="M 45 58 Q 50 82 55 58 Z" fill={P.corn} />
        <path d="M 47.5 58 Q 50 72 52.5 58 Z" fill="#FFF6D0" />
      </g>
    )}
    {/* fins */}
    <O d="M 37 40 C 28 44 25 52 26 62 L 38 55 Z" fill="url(#gLeaf)" filter="url(#bvLeaf)" ow={2.2} />
    <O d="M 63 40 C 72 44 75 52 74 62 L 62 55 Z" fill="url(#gLeafDark)" filter="url(#bvLeaf)" ow={2.2} />
    {/* nozzle */}
    <O d="M 43 54 H 57 L 55 60 H 45 Z" fill="url(#gInk)" ow={1.6} />
    {/* the body: a kernel, tip up */}
    <O d={BODY} fill="url(#gKernel)" filter="url(#bvCorn)" ow={2.6} />
    {/* a band */}
    <path d="M 31.5 44 Q 50 49 68.5 44" stroke={INK} strokeWidth={1.6} fill="none" opacity={0.55} />
    {/* centre fin, seen edge-on */}
    <O d="M 48.4 47 H 51.6 V 62 Q 50 63.5 48.4 62 Z" fill="url(#gLeaf)" ow={1.6} />
    {/* porthole with someone peeking */}
    <circle cx={50} cy={27} r={9.4} fill={INK} />
    <circle cx={50} cy={27} r={7.8} fill="url(#gCream)" />
    <circle cx={50} cy={27} r={6} fill="#2E3A44" />
    <Eye x={51.5} y={28} r={2.6} look={[0.6, -0.3]} />
    <path d="M 45.5 24.5 a 5 5 0 0 1 4 -2.6" stroke="#FFFFFF" strokeWidth={1.6} strokeLinecap="round" fill="none" opacity={0.75} />
    {/* rivets */}
    <g fill={INK} opacity={0.6}><circle cx={50} cy={16} r={0.9} /><circle cx={40} cy={30} r={0.9} /><circle cx={60} cy={30} r={0.9} /></g>
    <Shine d="M 38 20 q 2 -8 8 -12" w={2.6} />
  </g>
);

export const KernelRocketArt: React.FC<ArtProps> = ({ pose }) => {
  if (pose === "rest") {
    return (
      <g>
        <Rocket flame={false} />
        {/* launch-ready: a little steam at the base */}
        <Popcorn x={30} y={64} s={0.45} hull={false} opacity={0.9} />
        <Popcorn x={71} y={65} s={0.4} hull={false} opacity={0.9} />
      </g>
    );
  }
  return (
    <g>
      {/* the trail: popcorn puffs */}
      <Popcorn x={50} y={66} s={1.05} rot={10} />
      <Popcorn x={34} y={70} s={0.8} rot={-30} opacity={0.9} />
      <Popcorn x={67} y={71} s={0.75} rot={40} opacity={0.9} />
      <Popcorn x={22} y={64} s={0.55} rot={20} opacity={0.6} hull={false} />
      <Popcorn x={79} y={63} s={0.5} rot={-20} opacity={0.6} hull={false} />
      <g transform="translate(0 -9)">
        <Strokes d="M 22 30 v 14 M 78 26 v 16 M 30 52 v 8 M 70 50 v 10" w={2.4} />
        <Rocket flame />
      </g>
    </g>
  );
};
