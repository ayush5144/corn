import React from "react";
import { Puff } from "../lib";
import { ArtProps, INK, O, OKernel, Popcorn, Shine, Strokes } from "./shared";

// 4. Bucket Tip: a striped popcorn bucket, heaped full.
// Tap: it tips over, the popcorn tumbles out and fades away. Everything gone; the bucket stays.
const BODY = "M 30 27 L 36.5 62 Q 50 66.5 63.5 62 L 70 27 Z";

const Bucket: React.FC<{ full: boolean }> = ({ full }) => (
  <g>
    <defs><clipPath id="bucketBody"><path d={BODY} /></clipPath></defs>
    {full && (
      <g>
        <Puff lobes={[[34, 22, 7.5], [66, 22, 7.5], [42, 15, 8.5], [58, 14, 8.5], [50, 8, 8], [50, 20, 9]]} outline={INK} ow={2} />
      </g>
    )}
    <O d={BODY} fill="url(#gCream)" filter="url(#bvCream)" ow={2.6} />
    <g clipPath="url(#bucketBody)">
      {[0, 2, 4].map((i) => (
        <path key={i} d={`M ${30 + 8 * i} 26 L ${36.5 + 5.4 * i} 67 L ${36.5 + 5.4 * (i + 1)} 67 L ${30 + 8 * (i + 1)} 26 Z`}
          fill="url(#gCorn)" />
      ))}
      {[1, 2, 3, 4].map((i) => (
        <path key={`l${i}`} d={`M ${30 + 8 * i} 26 L ${36.5 + 5.4 * i} 67`} stroke={INK} strokeWidth={1} opacity={0.35} />
      ))}
      {/* body shading: darker to the right */}
      <path d="M 56 26 L 54 67 L 72 67 L 72 26 Z" fill="#7A4A00" opacity={0.18} />
    </g>
    <path d={BODY} fill="none" stroke={INK} strokeWidth={2.4} strokeLinejoin="round" />
    {/* rim */}
    <O d="M 28 25.5 Q 50 29 72 25.5 L 71.4 30 Q 50 33.5 28.6 30 Z" fill="url(#gCream)" ow={1.8} />
    {/* the label: a round badge with a kernel */}
    <circle cx={50} cy={46} r={7.4} fill={INK} />
    <circle cx={50} cy={46} r={6} fill="url(#gCream)" />
    <OKernel cx={50} cy={46} s={7.6} ow={1} />
    <Shine d="M 34 36 L 36.5 52" w={2} o={0.6} />
  </g>
);

export const BucketTipArt: React.FC<ArtProps> = ({ pose }) => {
  if (pose === "rest") {
    return (
      <g>
        <Bucket full />
        <Popcorn x={22} y={60} s={0.6} rot={-30} />
      </g>
    );
  }
  return (
    <g>
      {/* tipped onto its side, mouth to the left, rocking */}
      <g transform="translate(8 10) rotate(-84 50 46) translate(50 46) scale(.9) translate(-50 -46)">
        <Bucket full={false} />
        {/* looking into the mouth: dark inside, a few stragglers */}
        <ellipse cx={50} cy={27.4} rx={20.6} ry={6.6} fill={INK} />
        <ellipse cx={50} cy={27.4} rx={18.6} ry={5} fill="#4A3812" />
        <ellipse cx={50} cy={28.4} rx={16} ry={3.4} fill="#2A1E00" />
      </g>
      <Strokes d="M 60 18 q 10 0 14 8 M 66 70 q 10 -2 14 -10" w={2.4} />
      {/* popcorn tumbling out, bouncing off the edge */}
      <Popcorn x={24} y={56} s={0.85} rot={-15} />
      <Popcorn x={12} y={46} s={0.8} rot={40} />
      <Popcorn x={22} y={36} s={0.66} rot={70} />
      <Popcorn x={4} y={62} s={0.62} rot={-60} />
      <Popcorn x={-2} y={40} s={0.5} rot={100} />
      <Popcorn x={-6} y={74} s={0.42} rot={20} />
      <Strokes d="M 8 30 q -2 -5 2 -8 M -6 58 q -4 -2 -4 -6" w={1.8} />
    </g>
  );
};

