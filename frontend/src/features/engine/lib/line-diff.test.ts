import { describe, expect, it } from "vitest";
import { lineDiff } from "./line-diff";

describe("lineDiff", () => {
  it("marks added and removed lines and folds distant unchanged ones", () => {
    const before = ["intro", "a", "b", "c", "d", "trace line", "end"].join("\n");
    const after = ["intro", "a", "b", "c", "d", "```text", "trace line", "```", "end"].join("\n");

    expect(lineDiff(before, after)).toEqual([
      { type: "gap", text: "⋯" },
      { type: "same", text: "d" },
      { type: "added", text: "```text" },
      { type: "same", text: "trace line" },
      { type: "added", text: "```" },
      { type: "same", text: "end" },
    ]);
  });

  it("shows removals", () => {
    expect(lineDiff("keep\nlog 1\nlog 2", "keep", 0)).toEqual([
      { type: "gap", text: "⋯" },
      { type: "removed", text: "log 1" },
      { type: "removed", text: "log 2" },
    ]);
  });
});
