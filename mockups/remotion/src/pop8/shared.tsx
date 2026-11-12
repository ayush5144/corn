import React from "react";
import { P } from "../palette";
import { Canvas, Puff } from "../lib";
import { DOME, PopBase } from "../PopBase";

// Shared kit for the 8 Pop concepts ("pop8"). Every concept is drawn as a sticker:
// an ink outline (like Corny, the logo) around bevelled, gradient-lit shapes, light from
// the top-left. The art stands on the founder's push button (PopBase), corn-yellow dome.
//
// Art is drawn on a 100x100 grid; its "floor" (where it touches the dome) is y = 66.

export type Pose = "rest" | "action";
export type ArtProps = { pose: Pose; k?: number }; // k: 0..1 progress of the action (for animation)

export const INK = P.ink;
export const OW = 2.4; // outline half-width, in art units

// A path with an ink outline behind it.
export const O: React.FC<{
  d: string; fill: string; filter?: string; ow?: number; transform?: string; opacity?: number;
}> = ({ d, fill, filter, ow = OW, transform, opacity }) => (
  <g transform={transform} opacity={opacity}>
    <path d={d} fill={INK} stroke={INK} strokeWidth={ow * 2} strokeLinejoin="round" />
    <path d={d} fill={fill} filter={filter} />
  </g>
);

export const Shine: React.FC<{ d: string; w?: number; o?: number }> = ({ d, w = 2.4, o = 0.9 }) => (
  <path d={d} stroke="#FFFBEA" strokeWidth={w} strokeLinecap="round" fill="none" opacity={o} />
);

// Cartoon eyes: ink ovals with a white glint. look = [dx, dy] pupil offset.
export const Eye: React.FC<{ x: number; y: number; r?: number; look?: [number, number] }> = ({ x, y, r = 2.6, look = [0, 0] }) => (
  <g>
    <ellipse cx={x + look[0]} cy={y + look[1]} rx={r * 0.82} ry={r} fill={INK} />
    <circle cx={x + look[0] - r * 0.28} cy={y + look[1] - r * 0.38} r={r * 0.32} fill="#FFFFFF" />
  </g>
);
// A wink: a happy closed eye (^ arc).
export const Wink: React.FC<{ x: number; y: number; r?: number; w?: number }> = ({ x, y, r = 3, w = 1.9 }) => (
  <path d={`M ${x - r} ${y + r * 0.35} Q ${x} ${y - r * 0.9} ${x + r} ${y + r * 0.35}`} stroke={INK} strokeWidth={w}
    strokeLinecap="round" fill="none" />
);
export const Mouth: React.FC<{ d: string; w?: number; fill?: string }> = ({ d, w = 1.8, fill = "none" }) => (
  <path d={d} stroke={INK} strokeWidth={w} strokeLinecap="round" strokeLinejoin="round" fill={fill} />
);
export const Blush: React.FC<{ x: number; y: number; r?: number }> = ({ x, y, r = 2.6 }) => (
  <ellipse cx={x} cy={y} rx={r} ry={r * 0.6} fill="#FF8A5C" opacity={0.45} />
);

// Motion / burst strokes. Default: corn yellow with an ink outline, so they read on the dark
// toolbar, the light toolbar and the yellow dome alike. color=INK draws a plain ink stroke.
export const Strokes: React.FC<{ d: string; w?: number; color?: string; o?: number }> = ({ d, w = 2.4, color = P.corn, o = 1 }) => (
  <g opacity={o} fill="none" strokeLinecap="round" strokeLinejoin="round">
    {color !== INK && <path d={d} stroke={INK} strokeWidth={w + 2.2} />}
    <path d={d} stroke={color} strokeWidth={w} />
  </g>
);

// A plump corn kernel (like the logo's): wide crown with a soft dent, tapering to a round tip.
export const fatKernel = (cx: number, cy: number, s: number) => {
  const h = s * 0.4, top = cy - s / 2, bot = cy + s / 2;
  return `M ${cx - h * 0.9} ${top + s * 0.12} Q ${cx - h * 0.5} ${top - s * 0.03} ${cx} ${top + s * 0.05}
    Q ${cx + h * 0.5} ${top - s * 0.03} ${cx + h * 0.9} ${top + s * 0.12}
    C ${cx + h * 1.12} ${top + s * 0.25} ${cx + h * 1.05} ${cy + s * 0.18} ${cx + h * 0.5} ${bot - s * 0.07}
    Q ${cx} ${bot + s * 0.05} ${cx - h * 0.5} ${bot - s * 0.07}
    C ${cx - h * 1.05} ${cy + s * 0.18} ${cx - h * 1.12} ${top + s * 0.25} ${cx - h * 0.9} ${top + s * 0.12} Z`;
};

// A piece of popcorn: a cream puff with an ink outline and the golden hull peeking out below.
export const Popcorn: React.FC<{ x: number; y: number; s?: number; rot?: number; opacity?: number; hull?: boolean }> = ({
  x, y, s = 1, rot = 0, opacity = 1, hull = true,
}) => (
  <g transform={`translate(${x} ${y}) rotate(${rot}) scale(${s})`} opacity={opacity}>
    {hull && <O d="M -4.6 3.5 Q 0 12 4.6 3.5 Z" fill="url(#gKernel)" filter="url(#bvSmall)" ow={1.6} />}
    <Puff lobes={[[-3.6, -2.2, 4.4], [3.4, -2.8, 4.6], [-4.4, 2.6, 3.8], [4.2, 2.4, 3.8], [0, 0.4, 4.8]]}
      outline={INK} ow={1.6} />
  </g>
);

// A small kernel with an ink outline.
export const OKernel: React.FC<{ cx: number; cy: number; s: number; rot?: number; ow?: number; opacity?: number }> = ({
  cx, cy, s, rot = 0, ow = 1.8, opacity,
}) => (
  <g transform={`rotate(${rot} ${cx} ${cy})`} opacity={opacity}>
    <O d={fatKernel(cx, cy, s)} fill="url(#gKernel)" filter="url(#bvCorn)" ow={ow} />
    <Shine d={`M ${cx - s * 0.2} ${cy - s * 0.26} q ${s * 0.12} ${-s * 0.1} ${s * 0.26} ${-s * 0.07}`} w={s * 0.08} />
  </g>
);

// Speech-bubble lettering ("shh", "ding!"), drawn with a system font.
export const Say: React.FC<{ x: number; y: number; text: string; size?: number; rot?: number; w?: number }> = ({
  x, y, text, size = 9, rot = 0, w,
}) => {
  const bw = w ?? text.length * size * 0.58 + 7;
  const bh = size + 6;
  return (
    <g transform={`translate(${x} ${y}) rotate(${rot})`}>
      <O d={`M ${-bw / 2 + 4} ${-bh / 2} H ${bw / 2 - 4} Q ${bw / 2} ${-bh / 2} ${bw / 2} ${-bh / 2 + 4} V ${bh / 2 - 4}
        Q ${bw / 2} ${bh / 2} ${bw / 2 - 4} ${bh / 2} H ${-bw / 2 + 9} L ${-bw / 2 + 3} ${bh / 2 + 5} L ${-bw / 2 + 5} ${bh / 2}
        H ${-bw / 2 + 4} Q ${-bw / 2} ${bh / 2} ${-bw / 2} ${bh / 2 - 4} V ${-bh / 2 + 4} Q ${-bw / 2} ${-bh / 2} ${-bw / 2 + 4} ${-bh / 2} Z`}
        fill="url(#gCream)" ow={1.5} />
      <text x={0} y={size * 0.36} textAnchor="middle" fontFamily="'Arial Rounded MT Bold', 'Helvetica Neue', Arial, sans-serif"
        fontWeight={900} fontSize={size} fill={INK}>{text}</text>
    </g>
  );
};

// The stage: the push button with the art standing on its dome.
// under / over: drawn on the dome under / over the art (e.g. heat glow).
export const TIGHT = "0.5 4 99 86";
export const Stage: React.FC<{
  art: React.ReactNode; scale?: number; lift?: number; press?: number; tight?: boolean; bg?: string;
  under?: React.ReactNode; over?: React.ReactNode; arcs?: boolean; shadow?: boolean;
}> = ({ art, scale = 0.58, lift = 0, press = 0, tight = false, bg, under, over, arcs = true, shadow = true }) => {
  const cy = DOME.cy + press * 7;
  return (
    <Canvas bg={bg} viewBox={tight ? TIGHT : undefined}>
      <PopBase
        dome="corn"
        arcs={tight || !arcs ? null : P.corn}
        press={press}
        over={
          <>
            {under}
            <g transform={`translate(${DOME.cx} ${cy - 2 - lift}) scale(${scale}) translate(-50 -62)`}>
              {shadow && <ellipse cx={50} cy={66} rx={30} ry={7} fill="#000" opacity={0.3} filter="url(#soft)" />}
              <g filter="url(#drop)">{art}</g>
            </g>
            {over}
          </>
        }
      />
    </Canvas>
  );
};
