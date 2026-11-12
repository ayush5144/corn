import React from "react";
import { Still } from "remotion";
import { PopBurst } from "./PopBurst";
import { P } from "./palette";

export const Root: React.FC = () => (
  <>
    <Still id="pop-burst" component={PopBurst} width={1024} height={1024} />
    <Still id="pop-burst-light" component={PopBurst} defaultProps={{ bg: "#FBF8F1" }} width={1024} height={1024} />
    <Still id="pop-burst-on-yellow" component={PopBurst} defaultProps={{ bg: P.corn }} width={1024} height={1024} />
  </>
);
