import React from "react";
import { Puff } from "../lib";
import { ArtProps, INK, O, Popcorn, Shine, Strokes } from "./shared";

// 7. Popcorn Cart Lever: a little retro popcorn machine with a big lever on its side.
// Tap (or drag the lever down): the lever clunks down, the kettle tips, and the whole
// batch goes off. Fresh batch; the old one's gone.
export const CartLeverArt: React.FC<ArtProps> = ({ pose }) => {
  const pulled = pose === "action";
  return (
    <g>
      <defs>
        <linearGradient id="gGlass" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0" stopColor="#FFFFFF" stopOpacity={0.55} />
          <stop offset="1" stopColor="#FFF1C2" stopOpacity={0.25} />
        </linearGradient>
        <clipPath id="cartGlass"><rect x={29} y={22} width={38} height={27} rx={2} /></clipPath>
      </defs>

      {pulled && (
        <>
          <Strokes d="M 16 12 l 6 6 M 50 -4 v 6 M 8 32 h 8" w={2.8} />
          <Popcorn x={30} y={8} s={0.8} rot={-25} />
          <Popcorn x={62} y={4} s={0.75} rot={30} />
          <Popcorn x={46} y={-2} s={0.6} rot={5} />
          <Popcorn x={14} y={22} s={0.6} rot={-50} opacity={0.85} />
        </>
      )}

      {/* lever, behind the cabinet's right side */}
      <g transform={pulled ? "rotate(118 74 40)" : "rotate(0 74 40)"}>
        <path d="M 74 40 L 86 22" stroke={INK} strokeWidth={5.6} strokeLinecap="round" />
        <path d="M 74 40 L 86 22" stroke="url(#gZipL)" strokeWidth={3} strokeLinecap="round" />
        <circle cx={87} cy={20} r={5.6} fill={INK} />
        <circle cx={87} cy={20} r={4.2} fill="url(#gLeaf)" />
        <circle cx={85.6} cy={18.6} r={1.3} fill="#FFFFFF" opacity={0.8} />
      </g>
      <linearGradient id="gZipL" x1="0" y1="0" x2="1" y2="0">
        <stop offset="0" stopColor="#FFFFFF" /><stop offset="1" stopColor="#B8AE98" />
      </linearGradient>
      <O d="M 72 37 h 5 v 7 h -5 Z" fill="url(#gInk)" ow={1.2} />
      <circle cx={74.4} cy={40.4} r={1.6} fill="#D9D2C2" />

      {/* wheels */}
      {[34, 66].map((x) => (
        <g key={x}><circle cx={x} cy={63} r={5} fill={INK} /><circle cx={x} cy={63} r={2.6} fill="url(#gCream)" /></g>
      ))}
      {/* cabinet */}
      <O d="M 26 48 H 74 V 60 Q 74 62 72 62 H 28 Q 26 62 26 60 Z" fill="url(#gCorn)" filter="url(#bvCorn)" ow={2.4} />
      <O d="M 44 51 H 56 V 59 H 44 Z" fill="url(#gCream)" ow={1.2} />
      <circle cx={54} cy={55} r={0.9} fill={INK} />
      {/* glass box frame */}
      <O d="M 26 20 H 74 V 49 H 26 Z" fill="url(#gCream)" filter="url(#bvCream)" ow={2.4} />
      <rect x={29} y={22} width={38} height={27} rx={2} fill="#3A2C10" opacity={0.9} />
      <g clipPath="url(#cartGlass)">
        {/* inside: warm light, the heap, the kettle */}
        <ellipse cx={48} cy={30} rx={20} ry={12} fill="#FFB43D" opacity={0.45} filter="url(#soft)" />
        <Puff lobes={pulled
          ? [[31, 47, 5], [38, 44, 5.5], [46, 41, 6], [54, 43, 5.5], [62, 45, 5.5], [67, 46, 5], [42, 32, 4.5], [56, 33, 4.6], [49, 27, 4.2], [36, 26, 4]]
          : [[31, 48, 4.6], [38, 46, 5], [46, 45.5, 5.2], [54, 46, 5], [61, 46.5, 4.8], [67, 48, 4.6]]} outline={INK} ow={1.1} />
        <g transform={pulled ? "rotate(38 50 26)" : undefined}>
          <path d="M 50 22 V 25.5" stroke={INK} strokeWidth={1.6} />
          <O d="M 42 26 H 58 L 56.5 34 Q 50 36.5 43.5 34 Z" fill="url(#gInk)" filter="url(#bvInk)" ow={1.2} />
          <path d="M 42 26 H 58" stroke="#9A8A62" strokeWidth={1.2} strokeLinecap="round" />
        </g>
        {/* glass reflections */}
        <rect x={29} y={22} width={38} height={27} fill="url(#gGlass)" />
        <path d="M 33 22 L 29 30 M 39 22 L 29 41 M 64 49 L 67 43" stroke="#FFFFFF" strokeWidth={1.8} opacity={0.55} />
      </g>
      <path d="M 50 20 V 49" stroke={INK} strokeWidth={1.2} opacity={0.35} />
      {/* roof: a scalloped canopy with a sign and a finial */}
      <O d="M 22 21 Q 24 10 50 7 Q 76 10 78 21 Z" fill="url(#gCorn)" filter="url(#bvCorn)" ow={2.4} />
      <O d="M 22 21 H 78 Q 78 25 74 25 Q 70 25 70 21 Q 70 25 66 25 Q 62 25 62 21 Q 62 25 58 25 Q 54 25 54 21 Q 54 25 50 25 Q 46 25 46 21 Q 46 25 42 25 Q 38 25 38 21 Q 38 25 34 25 Q 30 25 30 21 Q 30 25 26 25 Q 22 25 22 21 Z"
        fill="url(#gCream)" ow={1.4} />
      <O d="M 39 10 H 61 Q 63 10 63 12 V 16 Q 63 18 61 18 H 39 Q 37 18 37 16 V 12 Q 37 10 39 10 Z" fill="url(#gCream)" ow={1.3} />
      <text x={50} y={16.6} textAnchor="middle" fontFamily="'Arial Rounded MT Bold', 'Helvetica Neue', Arial, sans-serif"
        fontWeight={900} fontSize={7} fill={INK} letterSpacing={0.6}>POP</text>
      <O d="M 50 1 Q 53.5 1 53.5 4.2 Q 53.5 7 50 7 Q 46.5 7 46.5 4.2 Q 46.5 1 50 1 Z" fill="url(#gCorn)" ow={1.3} />
      <Shine d="M 27 18 Q 30 13 36 11" w={1.8} o={0.7} />
      {pulled && <Strokes d="M 92 46 l 5 2 M 90 54 l 5 4" w={2.2} />}
    </g>
  );
};
