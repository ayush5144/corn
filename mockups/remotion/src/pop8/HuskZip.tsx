import React from "react";
import { P } from "../palette";
import { ArtProps, Eye, INK, Mouth, O, Shine, Strokes } from "./shared";

// 5. Husk Zip: a cob standing in its open husk, out in the open.
// Tap: the husk leaves fold up around it and a zipper runs up the seam.
// All that's left: two eyes peeking out of the top. Tucked away, nobody saw.
const COB = "M 50 7 C 59 7 63 16 63 28 V 52 C 63 61 58 66 50 66 C 42 66 37 61 37 52 V 28 C 37 16 41 7 50 7 Z";

const Kernels: React.FC = () => {
  const out: React.ReactNode[] = [];
  for (let r = 0; r < 8; r++) {
    const y = 6 + r * 7.4;
    for (let c = 0; c < 4; c++) {
      const x = 34.6 + c * 7.8 + (r % 2 ? 3.9 : 0) - 3.9;
      out.push(<rect key={`${r}-${c}`} x={x} y={y} width={7} height={6.8} rx={2.8} fill="url(#gKernel)" stroke={INK} strokeWidth={1.2} />);
    }
  }
  return <g>{out}</g>;
};

const Cob: React.FC<{ face?: "cheeky" | "peek" }> = ({ face }) => (
  <g>
    <defs><clipPath id="huskCob"><path d={COB} /></clipPath></defs>
    <O d={COB} fill="url(#gCorn)" filter="url(#bvCorn)" ow={2.6} />
    <g clipPath="url(#huskCob)"><Kernels /></g>
    <path d={COB} fill="none" stroke={INK} strokeWidth={2.4} />
    {face === "cheeky" && (
      <>
        {/* a smooth patch for the face, like the logo */}
        <path d="M 39 26 Q 50 22 61 26 V 42 Q 50 46 39 42 Z" fill="url(#gCorn)" />
        <path d="M 39 26 Q 50 22 61 26 M 39 42 Q 50 46 61 42" stroke={INK} strokeWidth={1.2} fill="none" opacity={0.5} />
        <Eye x={45} y={31} r={2.7} look={[-0.9, 0]} />
        <Eye x={55.5} y={31} r={2.7} look={[-0.9, 0]} />
        <Mouth d="M 45 38 Q 50.5 41.5 56 37" w={1.9} />
      </>
    )}
    {face === "peek" && (
      <>
        <path d="M 40 12 Q 50 9 60 12 V 22 Q 50 25 40 22 Z" fill="url(#gCorn)" />
        <Eye x={45.6} y={18.5} r={2.5} look={[-0.8, 0.3]} />
        <Eye x={54.2} y={18.5} r={2.5} look={[-0.8, 0.3]} />
      </>
    )}
    <Shine d="M 40.5 30 V 44" w={2} o={0.6} />
  </g>
);

export const HuskZipArt: React.FC<ArtProps> = ({ pose }) => {
  if (pose === "rest") {
    return (
      <g>
        <O d="M 48 67 C 24 67 10 50 6 18 C 22 26 34 38 42 52 Z" fill="url(#gLeafDark)" filter="url(#bvLeaf)" ow={2.4} />
        <O d="M 52 67 C 76 67 90 50 94 18 C 78 26 66 38 58 52 Z" fill="url(#gLeaf)" filter="url(#bvLeaf)" ow={2.4} />
        <path d="M 12 27 C 18 44 28 56 42 63 M 88 27 C 82 44 72 56 58 63" stroke={P.leafDeep} strokeWidth={1.3} fill="none" strokeLinecap="round" />
        <Cob face="cheeky" />
      </g>
    );
  }
  // zipped: the leaves wrap the cob; the zip stops just short of the top
  const L = "M 50 67 C 35 67 28 54 28 38 C 28 24 32 12 37 5 C 40 14 44 22 49 28 L 50 67 Z";
  const R = "M 50 67 C 65 67 72 54 72 38 C 72 24 68 12 63 5 C 60 14 56 22 51 28 L 50 67 Z";
  const teeth: React.ReactNode[] = [];
  for (let y = 30; y < 64; y += 3.2) {
    const left = Math.round((y - 30) / 3.2) % 2 === 0;
    teeth.push(<rect key={y} x={left ? 47.2 : 49.4} y={y} width={3.4} height={2.2} rx={0.8} fill="url(#gZip)" stroke={INK} strokeWidth={0.7} />);
  }
  return (
    <g>
      <defs>
        <linearGradient id="gZip" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0" stopColor="#FFFFFF" /><stop offset="0.5" stopColor="#D9D2C2" /><stop offset="1" stopColor="#8F8672" />
        </linearGradient>
      </defs>
      <Cob face="peek" />
      <O d={L} fill="url(#gLeafDark)" filter="url(#bvLeaf)" ow={2.4} />
      <O d={R} fill="url(#gLeaf)" filter="url(#bvLeaf)" ow={2.4} />
      <path d="M 34 30 C 34 44 38 56 46 62 M 66 30 C 66 44 62 56 54 62" stroke={P.leafDeep} strokeWidth={1.3} fill="none" strokeLinecap="round" />
      {/* the open bit above the slider */}
      <path d="M 49 28 L 50 31 L 51 28" stroke={INK} strokeWidth={1.4} fill="none" strokeLinejoin="round" />
      {teeth}
      {/* the slider and its pull tab, swinging */}
      <O d="M 46.6 27.6 H 53.4 L 52.4 33 H 47.6 Z" fill="url(#gZip)" ow={1.2} />
      <g transform="rotate(-24 50 32)">
        <O d="M 47.8 32 H 52.2 L 53 43 Q 50 45.5 47 43 Z" fill="url(#gZip)" ow={1.2} />
        <rect x={48.8} y={38} width={2.4} height={3.4} rx={1.2} fill={INK} />
      </g>
      <Strokes d="M 22 12 l 4 4 M 78 12 l -4 4" w={2.2} />
    </g>
  );
};
