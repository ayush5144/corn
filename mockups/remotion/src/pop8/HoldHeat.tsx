import React from "react";
import { DOME } from "../PopBase";
import { ArtProps, Eye, INK, Mouth, O, Shine, Strokes, fatKernel } from "./shared";

// 2. Hold to Heat: a kernel chilling in sunglasses on a cool button.
// Press and hold: the button heats up from the bottom (orange glow), the kernel sweats,
// its shades slide down its nose, it jiggles faster and faster... then POP.
export const HoldHeatArt: React.FC<ArtProps> = ({ pose, k = pose === "rest" ? 0 : 0.85 }) => {
  const hot = pose === "action";
  return (
    <g>
      {hot && (
        <>
          {/* jiggle marks */}
          <Strokes d="M 19 26 q -4 6 0 12 q 4 6 0 12 M 81 26 q 4 6 0 12 q -4 6 0 12" w={2.6} color="#FF8A3D" />
          <Strokes d="M 11 32 q -2.5 4 0 8 M 89 32 q 2.5 4 0 8" w={2.2} color="#FF8A3D" />
        </>
      )}
      <g transform={hot ? "translate(1.5 0) rotate(5 50 64)" : "rotate(-4 50 64)"}>
        <O d={fatKernel(50, 38, 54)} fill="url(#gKernel)" filter="url(#bvCorn)" ow={2.6} />
        {/* heat tint, rising from the tip */}
        {hot && (
          <>
            <defs>
              <linearGradient id="gKernelHot" x1="0" y1="1" x2="0" y2="0">
                <stop offset="0%" stopColor="#FF5A1F" stopOpacity={0.85 * k} />
                <stop offset="70%" stopColor="#FF8A3D" stopOpacity={0.35 * k} />
                <stop offset="100%" stopColor="#FF8A3D" stopOpacity={0} />
              </linearGradient>
            </defs>
            <path d={fatKernel(50, 38, 54)} fill="url(#gKernelHot)" />
          </>
        )}
        <Shine d="M 34 22 q 3 -7 10 -8.5" w={3.2} />
        {hot ? (
          <>
            {/* wide eyes peeking over the shades, which slid down the nose */}
            <Eye x={42.5} y={28.5} r={3.1} look={[0, -0.6]} />
            <Eye x={58} y={28.5} r={3.1} look={[0, -0.6]} />
            <Shades y={37.5} rot={-6} />
            <Mouth d="M 45 48 Q 50 43.5 55 48 Q 50 52.5 45 48 Z" w={2} fill={INK} />
            <O d="M 31 18 q 4 5.6 0 8.4 q -4 -2.8 0 -8.4 Z" fill="#DFF3FF" ow={1.3} />
            <O d="M 71 22 q 3.2 4.6 0 6.8 q -3.2 -2.2 0 -6.8 Z" fill="#DFF3FF" ow={1.3} />
            <O d="M 68 40 q 2.6 3.6 0 5.4 q -2.6 -1.8 0 -5.4 Z" fill="#DFF3FF" ow={1.1} />
          </>
        ) : (
          <>
            <Shades y={31} />
            {/* a relaxed smirk */}
            <Mouth d="M 44.5 43 Q 50.5 47 56 41.5" w={2.1} />
          </>
        )}
      </g>
    </g>
  );
};

const Shades: React.FC<{ y: number; rot?: number }> = ({ y, rot = 0 }) => (
  <g transform={`rotate(${rot} 50 ${y})`}>
    <path d={`M 34.5 ${y - 3.5} H 65.5`} stroke={INK} strokeWidth={2.4} strokeLinecap="round" />
    <path d={`M 35 ${y - 4} H 48 Q 48 ${y + 5} 41.5 ${y + 5} Q 35 ${y + 5} 35 ${y - 4} Z`} fill={INK} />
    <path d={`M 52 ${y - 4} H 65 Q 65 ${y + 5} 58.5 ${y + 5} Q 52 ${y + 5} 52 ${y - 4} Z`} fill={INK} />
    <path d={`M 37.5 ${y - 2} l 3 3 M 54.5 ${y - 2} l 3 3`} stroke="#FFFFFF" strokeOpacity={0.6} strokeWidth={1.2} strokeLinecap="round" />
  </g>
);

// The heat on the dome itself: an orange-red glow filling the side band and the top
// from the bottom up, a warm halo around the base and heat shimmer above. k: 0..1.
export const HeatGlow: React.FC<{ k: number; press?: number }> = ({ k, press = 0 }) => {
  if (k <= 0) return null;
  const sink = press * 7;
  const cy = DOME.cy + sink, side = DOME.side - sink * 0.9, { cx, rx, ry } = DOME;
  const bottom = cy + side + ry, top = cy - ry;
  const fill = `M ${cx - rx} ${cy} A ${rx} ${ry} 0 0 1 ${cx + rx} ${cy} V ${cy + side} A ${rx} ${ry} 0 0 1 ${cx - rx} ${cy + side} Z`;
  return (
    <g>
      <defs>
        <linearGradient id="gHeat" gradientUnits="userSpaceOnUse" x1="0" y1={bottom} x2="0" y2={top}>
          <stop offset="0" stopColor="#E8380F" stopOpacity={0.95} />
          <stop offset={Math.max(0.01, k * 0.55)} stopColor="#FF6A1F" stopOpacity={0.8} />
          <stop offset={Math.min(1, k * 0.95 + 0.04)} stopColor="#FF9A3D" stopOpacity={0} />
        </linearGradient>
      </defs>
      <ellipse cx={50} cy={70} rx={44} ry={20} fill="#FF6A1F" opacity={0.35 * k} filter="url(#soft)" />
      <path d={fill} fill="url(#gHeat)" style={{ mixBlendMode: "multiply" }} />
      <path d={fill} fill="url(#gHeat)" opacity={0.35} />
      {k > 0.5 && (
        <Strokes d="M 20 27 q -3 -4 0 -8 q 3 -4 0 -8 M 80 27 q 3 -4 0 -8 q -3 -4 0 -8" w={2.2} color="#FF8A3D" o={Math.min(1, (k - 0.5) * 3)} />
      )}
    </g>
  );
};
