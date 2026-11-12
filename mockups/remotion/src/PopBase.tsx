import React from "react";
import { P } from "./palette";

// The Pop button base, after the founder's pick (mockups/flush/button/button-press.png):
// a chunky push button seen from slightly above. Cream base ring, a dark well, the dome's
// darker side band, a broad domed top with a glossy highlight top-left, and two "signal"
// arcs at the top right. The Pop art sits on the dome as its face.
//
// dome: "corn" (corn yellow) or "night" (Corn's dark surface).
// face: "decal" = printed flat on the dome top (squashed into perspective, like the swirl);
//       "raised" = popping up off the dome, upright.
// `press` (0..1) pushes the dome down into the well (used by the animation).

export const DOME = { cx: 50, cy: 45, rx: 33, ry: 17, side: 12 };

export const PopBase: React.FC<{
  dome?: "corn" | "night";
  arcs?: string | null;
  face?: React.ReactNode;
  faceMode?: "decal" | "raised";
  faceScale?: number;
  faceLift?: number;
  press?: number;
  over?: React.ReactNode; // drawn above everything (burst lines etc)
}> = ({ dome = "corn", arcs = P.corn, face, faceMode = "decal", faceScale = 0.52, faceLift = 0, press = 0, over }) => {
  const sink = press * 7;
  const cy = DOME.cy + sink;
  const side = DOME.side - sink * 0.9;
  const { cx, rx, ry } = DOME;
  const night = dome === "night";
  const domeTop = night ? "url(#gDomeNight)" : "url(#gDomeCorn)";
  const domeSide = night ? "url(#gSideNight)" : "url(#gSideCorn)";
  const domePath = `M ${cx - rx} ${cy} A ${rx} ${ry} 0 0 1 ${cx + rx} ${cy} V ${cy + side}
     A ${rx} ${ry} 0 0 1 ${cx - rx} ${cy + side} Z`;

  return (
    <g>
      <defs>
        <radialGradient id="gDomeCorn" cx="38%" cy="30%" r="75%">
          <stop offset="0%" stopColor="#FFE7A0" />
          <stop offset="40%" stopColor={P.corn} />
          <stop offset="100%" stopColor={P.cornDeep} />
        </radialGradient>
        <linearGradient id="gSideCorn" x1="0" y1="0" x2="1" y2="0">
          <stop offset="0%" stopColor="#E59A00" />
          <stop offset="35%" stopColor="#F2A900" />
          <stop offset="100%" stopColor="#B87400" />
        </linearGradient>
        <radialGradient id="gDomeNight" cx="38%" cy="30%" r="75%">
          <stop offset="0%" stopColor="#4A4434" />
          <stop offset="45%" stopColor={P.surface} />
          <stop offset="100%" stopColor="#17150F" />
        </radialGradient>
        <linearGradient id="gSideNight" x1="0" y1="0" x2="1" y2="0">
          <stop offset="0%" stopColor="#2A261E" />
          <stop offset="40%" stopColor="#1C1A14" />
          <stop offset="100%" stopColor="#0B0A07" />
        </linearGradient>
        <linearGradient id="gRingTop" x1="0" y1="0" x2="0.3" y2="1">
          <stop offset="0%" stopColor="#FFFFFF" />
          <stop offset="100%" stopColor={P.cream} />
        </linearGradient>
        <linearGradient id="gRingSide" x1="0" y1="0" x2="1" y2="0">
          <stop offset="0%" stopColor="#E9E0C9" />
          <stop offset="40%" stopColor={P.creamShade} />
          <stop offset="100%" stopColor="#B9AA88" />
        </linearGradient>
        <clipPath id="domeTopClip">
          <ellipse cx={cx} cy={cy} rx={rx} ry={ry} />
        </clipPath>
      </defs>

      {/* contact shadow */}
      <ellipse cx={50} cy={83} rx={41} ry={9} fill="#000" opacity={0.35} filter="url(#soft)" />

      {/* cream base ring: side band, then top */}
      <path d={`M 9 63 V 70 A 41 18 0 0 0 91 70 V 63 Z`} fill="url(#gRingSide)" />
      <ellipse cx={50} cy={70} rx={41} ry={18} fill="url(#gRingSide)" />
      <ellipse cx={50} cy={63} rx={41} ry={18} fill="url(#gRingTop)" />
      <ellipse cx={50} cy={63} rx={40.2} ry={17.3} fill="none" stroke="#FFFFFF" strokeWidth={0.8} opacity={0.9} />
      {/* the dark well the dome sits in */}
      <ellipse cx={50} cy={61} rx={35.5} ry={16.5} fill="#0D0B07" />
      <ellipse cx={50} cy={61.6} rx={35.5} ry={16.5} fill="none" stroke="#000" strokeWidth={1.2} opacity={0.4} />

      {/* the dome: side band + top */}
      <path d={domePath} fill={domeSide} />
      {/* thin lip where the side meets the top */}
      <ellipse cx={cx} cy={cy + 1.1} rx={rx} ry={ry} fill={night ? "#0B0A07" : "#D48E00"} />
      <ellipse cx={cx} cy={cy} rx={rx} ry={ry} fill={domeTop} />
      {/* a soft dome: darker toward the rim (inner vignette) */}
      <ellipse cx={cx} cy={cy} rx={rx} ry={ry} fill="none" stroke={night ? "#000" : "#C98700"} strokeWidth={5}
        opacity={night ? 0.35 : 0.22} clipPath="url(#domeTopClip)" filter="url(#soft)" />

      {/* the face printed on the dome */}
      {face && faceMode === "decal" && (
        <g clipPath="url(#domeTopClip)">
          <g transform={`translate(${cx} ${cy + 0.5}) scale(${faceScale} ${faceScale * 0.62}) translate(-50 -50)`}>{face}</g>
        </g>
      )}

      {/* gloss: a highlight streak along the top-left rim and a soft sheen */}
      <path d={`M ${cx - rx * 0.78} ${cy - ry * 0.25} A ${rx * 0.86} ${ry * 0.8} 0 0 1 ${cx - rx * 0.22} ${cy - ry * 0.76}`}
        stroke="#FFFFFF" strokeWidth={2.6} strokeLinecap="round" fill="none" opacity={night ? 0.32 : 0.7} />
      <ellipse cx={cx - rx * 0.35} cy={cy - ry * 0.4} rx={rx * 0.32} ry={ry * 0.22} fill="#FFFFFF"
        opacity={night ? 0.06 : 0.16} filter="url(#soft)" />
      {/* side-band specular */}
      <path d={`M ${cx - rx * 0.9} ${cy + side * 0.55} A ${rx} ${ry} 0 0 0 ${cx - rx * 0.45} ${cy + side + ry * 0.82}`}
        stroke="#FFFFFF" strokeWidth={1.4} strokeLinecap="round" fill="none" opacity={night ? 0.12 : 0.3} />

      {face && faceMode === "raised" && (
        <g transform={`translate(${cx} ${cy - 2 - faceLift}) scale(${faceScale}) translate(-50 -62)`}>
          {/* contact shadow of the raised face on the dome */}
          <ellipse cx={50} cy={66} rx={34} ry={9} fill="#000" opacity={0.28} filter="url(#soft)" />
          <g filter="url(#drop)">{face}</g>
        </g>
      )}

      {/* signal arcs, top right */}
      {arcs && (
        <g stroke={arcs} strokeWidth={3.2} strokeLinecap="round" fill="none">
          <path d="M 71 15.5 A 12 12 0 0 1 81.5 25" />
          <path d="M 72.5 7.5 A 20 20 0 0 1 89.5 23.5" />
        </g>
      )}
      {over}
    </g>
  );
};
