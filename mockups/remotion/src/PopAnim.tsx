import React from "react";
import { Easing, interpolate, spring, useCurrentFrame, useVideoConfig } from "remotion";
import { P } from "./palette";
import { Canvas } from "./lib";
import { PopBase } from "./PopBase";
import { BurstFace } from "./faces";

// A 1.2 s "pop" of the Burst button (variant c): the dome is pressed down, springs back,
// the kernel jumps up as the puff blooms out behind it, burst lines flash, the arcs pulse.
const clamp = { extrapolateLeft: "clamp", extrapolateRight: "clamp" } as const;

export const PopAnim: React.FC<{ bg?: string }> = ({ bg = P.bg }) => {
  const f = useCurrentFrame();
  const { fps } = useVideoConfig();
  const down = interpolate(f, [6, 14], [0, 1], { ...clamp, easing: Easing.in(Easing.quad) });
  const up = spring({ frame: f - 14, fps, config: { damping: 9, stiffness: 220, mass: 0.6 } });
  const press = Math.max(0, down - up);
  const pop = spring({ frame: f - 16, fps, config: { damping: 7, stiffness: 160, mass: 0.7 } });
  const puffK = spring({ frame: f - 15, fps, config: { damping: 8, stiffness: 140, mass: 0.6 } });
  const linesK = interpolate(f, [20, 26, 46, 60], [0, 1, 1, 0], clamp);
  const arcs = interpolate(f, [18, 24, 44, 66], [0.3, 1, 1, 0.3], clamp);
  return (
    <Canvas bg={bg}>
      <PopBase
        dome="corn"
        arcs={null}
        face={<BurstFace tone="color" puffK={puffK} linesK={linesK} />}
        faceMode="raised"
        faceScale={0.3 + 0.2 * pop}
        faceLift={8 * pop - 3 * press}
        press={press}
        over={
          <g stroke={P.corn} strokeWidth={3.2} strokeLinecap="round" fill="none">
            <path d="M 71 15.5 A 12 12 0 0 1 81.5 25" opacity={arcs} />
            <path d="M 72.5 7.5 A 20 20 0 0 1 89.5 23.5" opacity={interpolate(arcs, [0.3, 1], [0.1, 1])} />
          </g>
        }
      />
    </Canvas>
  );
};
