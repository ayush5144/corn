import React from "react";
import { P } from "./palette";
import { Canvas, Kernel, Use, Variant, WithBadge, kernelPath } from "./lib";

// The Vault icon concepts. 100x100 grid, light from the top-left, transparent background.
// Every component takes variant (a/b/c) and use: "vault" | "save" (+ badge) | "saved" (check badge).

export type VaultConcept = "crib" | "husk" | "jar" | "safe";

// A few kernels piled up, used inside the crib and the jar.
const KernelPile: React.FC<{ spots: [number, number, number, number][] }> = ({ spots }) => (
  <g>
    {spots.map(([x, y, s, r], i) => (
      <Kernel key={i} cx={x} cy={y} s={s} rot={r} filter="url(#bvSmall)" shine={s > 8} />
    ))}
  </g>
);

// ---------------- 1. Corn crib ----------------
// The slatted barn farmers keep corn in. a: gable roof, cream slats (after vault-crib.png);
// b: barn (gambrel) roof, deep-corn slats that hold up on the light theme; c: a round crib.
const Crib: React.FC<{ v: Variant }> = ({ v }) => {
  if (v === "c") {
    // round wire crib: a cylinder with a cone roof
    const slats = [-0.86, -0.52, -0.17, 0.17, 0.52, 0.86];
    return (
      <g>
        <ellipse cx={50} cy={86} rx={30} ry={5} fill="#000" opacity={0.3} filter="url(#soft)" />
        {/* body: dark interior full of kernels */}
        <path d="M 22 44 V 80 A 28 7 0 0 0 78 80 V 44 Z" fill="url(#gInk)" />
        <clipPath id="cribRound"><path d="M 22 44 V 80 A 28 7 0 0 0 78 80 V 44 Z" /></clipPath>
        <g clipPath="url(#cribRound)">
          <KernelPile spots={[
            [29, 52, 9, -20], [38, 50, 9, 10], [50, 51, 9, -5], [62, 50, 9, 15], [71, 52, 9, -15],
            [26, 62, 9, 15], [35, 61, 9, -10], [45, 62, 9, 20], [55, 61, 9, -15], [65, 62, 9, 10], [74, 61, 9, -20],
            [30, 72, 9, -5], [40, 72, 9, 15], [50, 73, 9, -10], [60, 72, 9, 5], [70, 72, 9, 20],
            [35, 81, 9, 10], [46, 82, 9, -15], [57, 82, 9, 10], [67, 81, 9, -5],
          ]} />
        </g>
        {/* curved slats: wider in the middle, narrow at the sides (perspective) */}
        <g filter="url(#bvCream)">
          {slats.map((t, i) => {
            const x = 50 + t * 28, w = 6.2 * Math.sqrt(1 - t * t) + 1.6;
            const y2 = 80 + 7 * Math.sqrt(1 - t * t);
            return <rect key={i} x={x - w / 2} y={43} width={w} height={y2 - 43} rx={w / 2.4} fill="url(#gCream)" />;
          })}
        </g>
        {/* hoops */}
        <path d="M 22 58 A 28 7 0 0 0 78 58" stroke={P.cornDeep} strokeWidth={2.4} fill="none" />
        <path d="M 22 73 A 28 7 0 0 0 78 73" stroke={P.cornDeep} strokeWidth={2.4} fill="none" />
        {/* cone roof */}
        <path d="M 50 10 Q 53 11 56 14 L 84 42 Q 86 46 82 47 Q 50 56 18 47 Q 14 46 16 42 L 44 14 Q 47 11 50 10 Z"
          fill="url(#gCorn)" filter="url(#bvCorn)" />
        <path d="M 18 47 Q 50 56 82 47" stroke="#B87400" strokeWidth={1.6} fill="none" opacity={0.7} />
        <path d="M 46 16 L 26 38" stroke="#FFF2C0" strokeWidth={2.2} strokeLinecap="round" opacity={0.8} />
        {/* roof seams */}
        <path d="M 50 12 L 50 51 M 50 12 L 34 49 M 50 12 L 66 49" stroke="#D18F00" strokeWidth={0.9} opacity={0.6} />
        <circle cx={50} cy={10.5} r={3} fill={P.leaf} filter="url(#bvLeaf)" />
      </g>
    );
  }
  const deep = v === "b";
  const slatFill = deep ? "url(#gSlatDeep)" : "url(#gCream)";
  const slats = [17, 31.5, 46, 60.5, 75];
  return (
    <g>
      <defs>
        <linearGradient id="gSlatDeep" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0%" stopColor="#FFCF5A" />
          <stop offset="100%" stopColor="#D88E00" />
        </linearGradient>
        <linearGradient id="gEave" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor="#000" stopOpacity={0.45} />
          <stop offset="100%" stopColor="#000" stopOpacity={0} />
        </linearGradient>
      </defs>
      <ellipse cx={50} cy={88} rx={38} ry={4} fill="#000" opacity={0.3} filter="url(#soft)" />
      {/* interior */}
      <rect x={17} y={42} width={66} height={42} fill="url(#gInk)" />
      <KernelPile spots={[
        [28.5, 50, 10, -12], [28.5, 60, 10, 10], [28.5, 70, 10, -8], [28.5, 79, 9, 12],
        [43, 51, 10, 12], [43, 61, 10, -10], [43, 71, 10, 10], [43, 80, 9, -8],
        [57.5, 50, 10, -10], [57.5, 60, 10, 12], [57.5, 70, 10, -12], [57.5, 79, 9, 8],
        [72, 51, 10, 10], [72, 61, 10, -12], [72, 71, 10, 8], [72, 80, 9, -10],
      ]} />
      {/* slats */}
      <g filter={deep ? "url(#bvCorn)" : "url(#bvCream)"}>
        {slats.map((x, i) => <rect key={i} x={x} y={40} width={8} height={45} rx={2.2} fill={slatFill} />)}
      </g>
      {/* shadow under the eave */}
      <rect x={17} y={40} width={66} height={8} fill="url(#gEave)" />
      {/* floor beam */}
      <rect x={12} y={83} width={76} height={6} rx={3} fill={deep ? "url(#gCream)" : "url(#gCorn)"} filter={deep ? "url(#bvCream)" : "url(#bvCorn)"} />
      {/* roof */}
      {deep ? (
        <>
          <path d="M 8 45 Q 6 42 9 39 L 20 24 Q 22 21 26 20 L 44 13 Q 50 11 56 13 L 74 20 Q 78 21 80 24 L 91 39 Q 94 42 92 45 Z"
            fill="url(#gCream)" filter="url(#bvCream)" />
          <path d="M 13 40 L 23 26 L 45 17.5" stroke="#FFFFFF" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round" fill="none" opacity={0.9} />
          {/* hay-loft door */}
          <path d="M 43 40 V 30 Q 50 23 57 30 V 40 Z" fill="url(#gInk)" />
          <path d="M 44.5 31 L 55.5 39 M 55.5 31 L 44.5 39" stroke={P.cornDeep} strokeWidth={1.6} strokeLinecap="round" />
        </>
      ) : (
        <>
          <path d="M 50 11 Q 53 11 55.5 13.5 L 90 39 Q 93 42 90 45 L 10 45 Q 7 42 10 39 L 44.5 13.5 Q 47 11 50 11 Z"
            fill="url(#gCorn)" filter="url(#bvCorn)" />
          <path d="M 45 16.5 L 15 39" stroke="#FFF2C0" strokeWidth={2.2} strokeLinecap="round" opacity={0.85} />
          {/* a single kernel in the gable, like a crest */}
          <path d={kernelPath(50, 32, 11)} fill="url(#gInk)" />
        </>
      )}
    </g>
  );
};

// ---------------- 2. Tucked cob ----------------
// a: the cob tucked into its husk, kernels showing (after vault-husk.png);
// b: wrapped right up, silk tassel on top and a twine tie; c: husk closed with a kernel lock.
const Husk: React.FC<{ v: Variant }> = ({ v }) => {
  const cob = "M 50 12 C 64 12, 68 30, 68 50 C 68 70, 62 82, 50 84 C 38 82, 32 70, 32 50 C 32 30, 36 12, 50 12 Z";
  const dots: React.ReactNode[] = [];
  for (let r = 0; r < 12; r++) {
    for (let c = -3; c <= 3; c++) {
      const x = 50 + c * 7 + (r % 2 ? 3.5 : 0), y = 17 + r * 6;
      dots.push(<ellipse key={`${r}${c}`} cx={x} cy={y} rx={3} ry={2.6} fill="url(#gKernel)" />);
    }
  }
  const backL = "M 47 92 C 24 86, 14 60, 12 24 C 28 34, 42 50, 50 74 Z";
  const backR = "M 53 92 C 76 86, 86 60, 88 24 C 72 34, 58 50, 50 74 Z";
  const frontR = "M 44 92 C 68 92, 82 66, 86 24 C 68 36, 52 56, 42 80 Z";
  const closedL = "M 52 92 C 26 90, 16 62, 22 36 C 24 26, 28 18, 33 12 C 38 34, 46 62, 54 90 Z";
  const closedR = "M 48 92 C 74 90, 84 62, 78 36 C 76 26, 72 18, 67 12 C 62 34, 54 62, 46 90 Z";
  return (
    <g>
      <ellipse cx={50} cy={90} rx={26} ry={4} fill="#000" opacity={0.3} filter="url(#soft)" />
      {v === "a" && (
        <>
          <path d={backL} fill="url(#gLeafDark)" filter="url(#bvLeaf)" />
          <clipPath id="cobClip"><path d={cob} /></clipPath>
          <path d={cob} fill="#D99200" />
          <g clipPath="url(#cobClip)" filter="url(#bvSmall)">{dots}</g>
          <path d="M 40 22 C 37 30, 36 40, 36 48" stroke="#FFF6D6" strokeWidth={2.4} strokeLinecap="round" fill="none" opacity={0.75} />
          <path d={frontR} fill="url(#gLeaf)" filter="url(#bvLeaf)" />
          <path d="M 46 88 C 60 80, 74 60, 82 32" stroke="#3E6E18" strokeWidth={1.1} fill="none" opacity={0.55} />
          <path d="M 18 34 C 24 52, 34 70, 44 84" stroke="#3E6E18" strokeWidth={1.1} fill="none" opacity={0.5} />
        </>
      )}
      {v !== "a" && (
        <>
          {/* the cob, mostly tucked away: only its top shows where the husk parts */}
          <clipPath id="cobClip"><path d={cob} /></clipPath>
          <path d={cob} fill="#D99200" />
          <g clipPath="url(#cobClip)" filter="url(#bvSmall)">{dots}</g>
          <path d={closedL} fill="url(#gLeafDark)" filter="url(#bvLeaf)" />
          <path d={closedR} fill="url(#gLeaf)" filter="url(#bvLeaf)" />
          <path d="M 29 30 C 25 44, 25 62, 33 78" stroke="#D8F5A8" strokeWidth={2.2} strokeLinecap="round" fill="none" opacity={0.55} />
          <path d="M 66 36 C 68 52, 66 68, 58 82" stroke="#2F5A12" strokeWidth={1.1} fill="none" opacity={0.5} />
          {/* silk tassel */}
          <g stroke={P.cornLight} strokeWidth={1.8} strokeLinecap="round" fill="none">
            <path d="M 50 13 C 47 7, 42 6, 39 9" /><path d="M 50 13 C 52 6, 57 5, 61 8" /><path d="M 50 13 C 49 8, 47 5, 45 4" />
          </g>
        </>
      )}
      {v === "b" && (
        <g>
          {/* twine tie */}
          <path d="M 29 62 Q 50 70 71 62" stroke="#B87400" strokeWidth={5} strokeLinecap="round" fill="none" />
          <path d="M 29 62 Q 50 70 71 62" stroke={P.corn} strokeWidth={3} strokeLinecap="round" fill="none" />
          <path d="M 50 66 C 42 72, 38 80, 40 86 M 50 66 C 56 74, 62 78, 64 84" stroke={P.corn} strokeWidth={2.6} strokeLinecap="round" fill="none" />
          <circle cx={50} cy={66} r={4.2} fill="url(#gCorn)" filter="url(#bvSmall)" />
        </g>
      )}
      {v === "c" && (
        <g>
          {/* a kernel-shaped lock where the husk crosses */}
          <path d="M 38 56 Q 38 48 46 48 H 54 Q 62 48 62 56" stroke="#D9CFB4" strokeWidth={3.6} fill="none" />
          <path d={kernelPath(50, 66, 24)} fill="url(#gCorn)" filter="url(#bvCorn)" />
          <circle cx={50} cy={62} r={2.6} fill={P.ink} />
          <path d="M 48.8 63 L 47.8 70 H 52.2 L 51.2 63 Z" fill={P.ink} />
        </g>
      )}
    </g>
  );
};

// ---------------- 3. Kernel jar ----------------
// a: tall mason jar, green lid (after vault-jar.png); b: round clamp-top jar with a wire bail;
// c: short jar, cream lid with a kernel knob and a label band.
const Jar: React.FC<{ v: Variant }> = ({ v }) => {
  const body = v === "b"
    ? "M 30 30 H 70 Q 82 34 84 52 V 74 Q 84 90 66 90 H 34 Q 16 90 16 74 V 52 Q 18 34 30 30 Z"
    : v === "c"
      ? "M 22 34 Q 22 28 28 28 H 72 Q 78 28 78 34 V 80 Q 78 90 66 90 H 34 Q 22 90 22 80 Z"
      : "M 26 25 H 74 Q 80 25 80 32 V 80 Q 80 90 68 90 H 32 Q 20 90 20 80 V 32 Q 20 25 26 25 Z";
  const pileTop = v === "a" ? 40 : 46;
  const spots: [number, number, number, number][] = [];
  let k = 0;
  for (let y = 86; y > pileTop; y -= 8.4) {
    const row = Math.round((86 - y) / 8.4);
    for (let x = 24 + (row % 2) * 5; x < 80; x += 10) {
      k++;
      spots.push([x + ((k * 37) % 5) - 2, y + ((k * 13) % 3) - 1, 11, ((k * 47) % 60) - 30]);
    }
  }
  return (
    <g>
      <defs>
        <clipPath id="jarClip"><path d={body} /></clipPath>
        <linearGradient id="gGlass" x1="0" y1="0" x2="1" y2="0">
          <stop offset="0%" stopColor="#FFFFFF" stopOpacity={0.32} />
          <stop offset="40%" stopColor="#FFF8E6" stopOpacity={0.14} />
          <stop offset="100%" stopColor="#E9DCB8" stopOpacity={0.3} />
        </linearGradient>
      </defs>
      <ellipse cx={50} cy={92} rx={32} ry={4} fill="#000" opacity={0.3} filter="url(#soft)" />
      {/* glass back + kernels */}
      <path d={body} fill="#3A3324" opacity={0.55} />
      <g clipPath="url(#jarClip)"><KernelPile spots={spots} /></g>
      {/* glass front */}
      <path d={body} fill="url(#gGlass)" />
      <path d={body} fill="none" stroke="#E8DCBC" strokeWidth={2.4} />
      <path d={body} fill="none" stroke="#8C7A50" strokeWidth={0.8} opacity={0.6} transform="translate(0.6 0.8)" />
      {/* glass highlights */}
      <path d={v === "b" ? "M 25 50 V 76" : "M 28 36 V 78"} stroke="#FFFFFF" strokeWidth={4} strokeLinecap="round" opacity={0.85} />
      <path d={v === "b" ? "M 25 44 V 46" : "M 34 36 V 46"} stroke="#FFFFFF" strokeWidth={2} strokeLinecap="round" opacity={0.6} />
      {v === "a" && (
        <>
          <rect x={24} y={20} width={52} height={7} rx={2} fill="#D9CFB4" />
          <rect x={20} y={8} width={60} height={14} rx={5} fill="url(#gLeaf)" filter="url(#bvLeaf)" />
          <path d="M 24 12.5 H 64" stroke="#D8F5A8" strokeWidth={1.8} strokeLinecap="round" opacity={0.7} />
          <g stroke="#4A7E20" strokeWidth={1} opacity={0.6}>
            {[28, 36, 44, 52, 60, 68, 74].map((x) => <path key={x} d={`M ${x} 16 V 20`} />)}
          </g>
        </>
      )}
      {v === "b" && (
        <>
          {/* glass lid + green gasket + wire bail */}
          <rect x={27} y={27} width={46} height={4} rx={2} fill={P.leaf} />
          <path d="M 27 28 Q 27 16 50 15 Q 73 16 73 28 Z" fill="url(#gCream)" filter="url(#bvCream)" />
          <path d="M 33 23 Q 38 18 47 17.5" stroke="#FFFFFF" strokeWidth={2} strokeLinecap="round" fill="none" />
          <path d="M 22 40 L 32 14 Q 50 8 68 14 L 78 40" stroke={P.ink} strokeWidth={2.6} strokeLinejoin="round" fill="none" />
          <path d="M 22 40 L 32 14 Q 50 8 68 14 L 78 40" stroke="#B8A57C" strokeWidth={1} strokeLinejoin="round" fill="none" />
          <circle cx={22} cy={40} r={2.4} fill={P.ink} /><circle cx={78} cy={40} r={2.4} fill={P.ink} />
        </>
      )}
      {v === "c" && (
        <>
          {/* label band with a kernel emblem */}
          <rect x={22} y={52} width={56} height={18} fill="url(#gCream)" />
          <rect x={22} y={52} width={56} height={18} fill="none" stroke={P.cornDeep} strokeWidth={1.2} />
          <path d={kernelPath(50, 61, 13)} fill="url(#gCorn)" filter="url(#bvSmall)" />
          <rect x={18} y={17} width={64} height={13} rx={5} fill="url(#gCorn)" filter="url(#bvCorn)" />
          <path d="M 22 21 H 64" stroke="#FFF2C0" strokeWidth={1.8} strokeLinecap="round" opacity={0.85} />
          <path d={kernelPath(50, 12, 11)} fill="url(#gKernel)" stroke="#B87400" strokeWidth={1} filter="url(#bvSmall)" />
        </>
      )}
    </g>
  );
};

// ---------------- 4. Kernel safe ----------------
// a: yellow frame, dark door, a kernel-shaped lock with a keyhole (after vault-safe.png);
// b: a round dial ringed with kernels, like a cob cut across; c: a dark safe on feet with a kernel lock and a handle.
const Safe: React.FC<{ v: Variant }> = ({ v }) => {
  const frame = v === "c" ? "url(#gNight)" : "url(#gCorn)";
  const door = v === "c" ? "url(#gCorn)" : "url(#gNight)";
  return (
    <g>
      <ellipse cx={50} cy={91} rx={36} ry={4} fill="#000" opacity={0.3} filter="url(#soft)" />
      {v === "c" && (
        <g fill="url(#gNight)">
          <rect x={18} y={82} width={12} height={8} rx={2.5} /><rect x={70} y={82} width={12} height={8} rx={2.5} />
        </g>
      )}
      <rect x={12} y={12} width={76} height={v === "c" ? 72 : 76} rx={17} fill={frame} filter={v === "c" ? "url(#bvInk)" : "url(#bvCorn)"} />
      {v === "c" && <rect x={12.6} y={12.6} width={74.8} height={70.8} rx={16.5} fill="none" stroke={P.cornDeep} strokeWidth={1.4} opacity={0.8} />}
      <rect x={21} y={21} width={58} height={v === "c" ? 54 : 58} rx={10} fill={door} filter={v === "c" ? "url(#bvCorn)" : "url(#bvInk)"} />
      {/* door inset shadow */}
      <rect x={21} y={21} width={58} height={v === "c" ? 54 : 58} rx={10} fill="none" stroke="#000" strokeOpacity={0.35} strokeWidth={1.2} />
      {/* hinges */}
      <g fill={v === "c" ? "#4A4434" : P.cornDeep} stroke="#000" strokeOpacity={0.3} strokeWidth={0.6}>
        <rect x={17} y={28} width={6} height={11} rx={1.6} /><rect x={17} y={v === "c" ? 57 : 61} width={6} height={11} rx={1.6} />
      </g>
      <path d="M 18 17 Q 30 14.5 44 14.5" stroke="#FFF2C0" strokeWidth={2} strokeLinecap="round" fill="none" opacity={v === "c" ? 0.2 : 0.8} />
      {v === "a" && (
        <g>
          <path d={kernelPath(52, 50, 38)} fill="url(#gKernel)" filter="url(#bvCorn)" />
          <circle cx={52} cy={46} r={3.6} fill={P.ink} />
          <path d="M 50.4 47.5 L 49 57 H 55 L 53.6 47.5 Z" fill={P.ink} />
          <path d="M 44 38 q 3 -4 8 -4.5" stroke="#FFF6D6" strokeWidth={2} strokeLinecap="round" fill="none" />
        </g>
      )}
      {v === "b" && (
        <g>
          {/* dial: a ring of kernels (a cob cut across), hub with a kernel pointer */}
          <circle cx={52} cy={50} r={20} fill="#3A3324" />
          {Array.from({ length: 12 }).map((_, i) => {
            const a = (i / 12) * Math.PI * 2;
            return <Kernel key={i} cx={52 + Math.cos(a) * 14.5} cy={50 + Math.sin(a) * 14.5} s={9} rot={(a * 180) / Math.PI - 90}
              filter="url(#bvSmall)" shine={false} />;
          })}
          <circle cx={52} cy={50} r={8} fill="url(#gCream)" filter="url(#bvCream)" />
          <path d="M 52 44 V 50" stroke={P.ink} strokeWidth={2.6} strokeLinecap="round" />
          <path d="M 52 25 l -2.6 -4 h 5.2 Z" fill={P.leaf} />
        </g>
      )}
      {v === "c" && (
        <g>
          <path d={kernelPath(44, 47, 30)} fill="url(#gInk)" filter="url(#bvInk)" />
          <circle cx={44} cy={44} r={3} fill={P.corn} />
          <path d="M 42.7 45.2 L 41.6 53 H 46.4 L 45.3 45.2 Z" fill={P.corn} />
          {/* handle */}
          <rect x={64} y={36} width={6} height={24} rx={3} fill="url(#gNight)" filter="url(#bvInk)" />
        </g>
      )}
    </g>
  );
};

const PARTS = { crib: Crib, husk: Husk, jar: Jar, safe: Safe };

export const Vault: React.FC<{ concept: VaultConcept; variant: Variant; use?: Use; bg?: string }> = ({
  concept, variant, use = "vault", bg,
}) => {
  const C = PARTS[concept];
  return (
    <Canvas bg={bg}>
      <WithBadge use={use}><C v={variant} /></WithBadge>
    </Canvas>
  );
};
