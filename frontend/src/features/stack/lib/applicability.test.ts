import { describe, expect, it } from "vitest";
import { declaredLowerBound, rangeLabel, rowFromDependency, rowsToRanges, sameRanges } from "./applicability";

describe("applicability helpers", () => {
  it("describes the half-open range in words", () => {
    expect(rangeLabel({ ecosystem: "NPM", name: "react", fromVersion: "18.0.0", untilVersion: "19.0.0" })).toBe("18.0.0 이상 · 19.0.0 미만");
    expect(rangeLabel({ ecosystem: "NPM", name: "react", fromVersion: null, untilVersion: "19.0.0" })).toBe("19.0.0 미만");
  });

  it("reads a declared npm range as its lower bound", () => {
    expect(declaredLowerBound("^3.2.0")).toBe("3.2.0");
    expect(declaredLowerBound("~1.4")).toBe("1.4");
    expect(declaredLowerBound(null)).toBe("");
    expect(rowFromDependency({ ecosystem: "NPM", name: "next", version: "^16.0.1" })).toEqual({
      ecosystem: "NPM",
      name: "next",
      fromVersion: "16.0.1",
      untilVersion: "",
    });
  });

  it("drops nameless rows and turns blank bounds into null", () => {
    const ranges = rowsToRanges([
      { ecosystem: "MAVEN", name: "  ", fromVersion: "1.0", untilVersion: "" },
      { ecosystem: "NPM", name: " react ", fromVersion: " 18.0.0 ", untilVersion: " " },
    ]);

    expect(ranges).toEqual([{ ecosystem: "NPM", name: "react", fromVersion: "18.0.0", untilVersion: null }]);
    expect(sameRanges(ranges, [{ ecosystem: "NPM", name: "react", fromVersion: "18.0.0", untilVersion: null }])).toBe(true);
  });
});
