import React from "react";
import { Still } from "remotion";
import { Pose, Stage } from "./shared";
import { KernelPopArt } from "./KernelPop";
import { HeatGlow, HoldHeatArt } from "./HoldHeat";
import { CornyShhArt } from "./CornyShh";
import { BucketTipArt } from "./BucketTip";
import { HuskZipArt } from "./HuskZip";
import { KernelRocketArt } from "./KernelRocket";
import { CartLeverArt } from "./CartLever";
import { NibbledCobArt } from "./NibbledCob";

// The 8 Pop concepts. Each renders three stills (transparent background):
//   pop8-<id>          1024 px, the art at rest on the push button
//   pop8-<id>-action   1024 px, the key moment of the tap
//   pop8-<id>-btn      1150x1000 (46:40), the rest pose cropped to the button, for the toolbar
export const IDS = ["kernel", "heat", "shh", "bucket", "zip", "rocket", "lever", "nibble"] as const;
export type ConceptId = (typeof IDS)[number];

type Def = {
  art: React.FC<{ pose: Pose; k?: number }>;
  scale?: number; actionScale?: number; actionLift?: number; press?: number; actionPress?: number;
  under?: (pose: Pose) => React.ReactNode; arcs?: (pose: Pose) => boolean;
};

export const CONCEPTS: Record<ConceptId, Def> = {
  kernel: { art: KernelPopArt, scale: 0.6, actionScale: 0.6 },
  heat: {
    art: HoldHeatArt, scale: 0.58, actionPress: 0.35,
    under: (p) => <HeatGlow k={p === "rest" ? 0 : 0.85} press={p === "rest" ? 0 : 0.35} />, arcs: (p) => p === "rest",
  },
  shh: { art: CornyShhArt, scale: 0.6 },
  bucket: { art: BucketTipArt, scale: 0.6 },
  zip: { art: HuskZipArt, scale: 0.6 },
  rocket: { art: KernelRocketArt, scale: 0.6 },
  lever: { art: CartLeverArt, scale: 0.6, actionPress: 0.15 },
  nibble: { art: NibbledCobArt, scale: 0.62 },
};

export const Pop8: React.FC<{ concept: ConceptId; pose: Pose; tight?: boolean; bg?: string }> = ({ concept, pose, tight, bg }) => {
  const d = CONCEPTS[concept];
  const Art = d.art;
  const act = pose === "action";
  return (
    <Stage tight={tight} bg={bg} art={<Art pose={pose} />}
      scale={act ? d.actionScale ?? d.scale : d.scale} lift={act ? d.actionLift ?? 0 : 0}
      press={act ? d.actionPress ?? 0 : d.press ?? 0} under={d.under?.(pose)} arcs={d.arcs ? d.arcs(pose) : true} />
  );
};

export const Pop8Stills: React.FC = () => (
  <>
    {IDS.flatMap((c) => [
      <Still key={c} id={`pop8-${c}`} component={Pop8 as any} defaultProps={{ concept: c, pose: "rest" }} width={1024} height={1024} />,
      <Still key={`${c}-a`} id={`pop8-${c}-action`} component={Pop8 as any} defaultProps={{ concept: c, pose: "action" }} width={1024} height={1024} />,
      <Still key={`${c}-b`} id={`pop8-${c}-btn`} component={Pop8 as any} defaultProps={{ concept: c, pose: "rest", tight: true }} width={1150} height={1000} />,
    ])}
  </>
);
