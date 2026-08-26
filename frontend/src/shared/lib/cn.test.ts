import { describe, expect, it } from "vitest";
import { cn } from "./cn";

describe("cn", () => {
  it("merges truthy class names", () => {
    expect(cn("a", false && "b", "c")).toBe("a c");
  });
});
