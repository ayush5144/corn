import React from "react";
import { Composition, Still } from "remotion";
import { PopAnim } from "./PopAnim";
import { Pop, PopGlyph, PopConcept } from "./Pop";
import { P } from "./palette";
import { Vault, VaultConcept } from "./Vault";
import { Pop8Stills } from "./pop8";

// One <Still> per rendered PNG. IDs are the output file names (out/<id>.png).
const S = 1024;
const variants = ["a", "b", "c"] as const;
const pops: PopConcept[] = ["burst", "puff", "cob", "kernel"];
const vaults: VaultConcept[] = ["crib", "husk", "jar", "safe"];
const uses = [["vault", ""], ["save", "-save"], ["saved", "-saved"]] as const;

export const Root: React.FC = () => (
  <>
    {pops.flatMap((c) =>
      variants.map((v) => (
        <Still key={`pop-${c}-${v}`} id={`pop-${c}-${v}`} component={Pop as any} defaultProps={{ concept: c, variant: v }} width={S} height={S} />
      )),
    )}
    {pops.flatMap((c) =>
      variants.map((v) => (
        <Still key={`t-${c}-${v}`} id={`pop-${c}-${v}-tight`} component={Pop as any} defaultProps={{ concept: c, variant: v, tight: true }} width={1150} height={1000} />
      )),
    )}
    {pops.map((c) => (
      <Still key={`glyph-${c}`} id={`pop-${c}-glyph`} component={PopGlyph as any} defaultProps={{ concept: c }} width={S} height={S} />
    ))}
    {pops.map((c) => (
      <Still key={`glyphy-${c}`} id={`pop-${c}-glyph-on-yellow`} component={PopGlyph as any} defaultProps={{ concept: c, bg: P.corn }} width={S} height={S} />
    ))}
    {vaults.flatMap((c) =>
      variants.flatMap((v) =>
        uses.map(([u, suffix]) => (
          <Still key={`vault-${c}-${v}${suffix}`} id={`vault-${c}-${v}${suffix}`} component={Vault as any}
            defaultProps={{ concept: c, variant: v, use: u }} width={S} height={S} />
        )),
      ),
    )}
    <Pop8Stills />
    <Composition id="pop-anim" component={PopAnim} durationInFrames={72} fps={60} width={512} height={512} />
  </>
);
