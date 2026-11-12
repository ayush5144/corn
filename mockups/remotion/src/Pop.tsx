import React from "react";
import { P } from "./palette";
import { Canvas, Variant } from "./lib";
import { PopBase } from "./PopBase";
import { BurstFace, CobFace, KernelFace, PuffFace, Tone } from "./faces";

export type PopConcept = "burst" | "puff" | "cob" | "kernel";

const face = (concept: PopConcept, tone: Tone, v: Variant) => {
  switch (concept) {
    case "burst": return <BurstFace tone={tone} lines={v === "c"} />;
    case "puff": return <PuffFace tone={tone} lines={v === "c"} />;
    case "cob": return <CobFace tone={tone} chunky={v === "c"} />;
    case "kernel": return <KernelFace tone={tone} />;
  }
};

// Every Pop concept on the push-button base. Variations:
//  a "Corn dome":  corn-yellow dome, the art printed on it two-tone (ink + cream).
//  b "Night dome": dark dome, the art in full colour, leaf-green signal arcs.
//  c "Big pop":    corn-yellow dome, the art in full colour popping up off the dome, bigger.
// tight: cropped to the button (46:40, no signal arcs), for use as the whole toolbar button.
export const TIGHT = "0.5 4 99 86";
export const Pop: React.FC<{ concept: PopConcept; variant: Variant; bg?: string; press?: number; tight?: boolean }> = ({
  concept, variant, bg, press = 0, tight = false,
}) => {
  const big = variant === "c";
  return (
    <Canvas bg={bg} viewBox={tight ? TIGHT : undefined}>
      <PopBase
        dome={variant === "b" ? "night" : "corn"}
        arcs={tight ? null : variant === "b" ? P.leaf : P.corn}
        face={face(concept, big ? "color" : variant === "a" ? "duo" : "color", variant)}
        faceMode={big ? "raised" : "decal"}
        faceScale={big ? (concept === "cob" ? 0.6 : concept === "burst" ? 0.5 : 0.55) : concept === "cob" ? 0.66 : 0.62}
        press={press}
      />
    </Canvas>
  );
};

// The flat two-tone glyph for today's flat corn-yellow Pop button (no 3D base).
export const PopGlyph: React.FC<{ concept: PopConcept; bg?: string }> = ({ concept, bg }) => (
  <Canvas bg={bg}>
    <g transform="translate(50 50) scale(0.94) translate(-50 -50)">{face(concept, "duo", "c")}</g>
  </Canvas>
);
