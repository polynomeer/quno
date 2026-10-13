import { describe, expect, it } from "vitest";
import { userLabel } from "./user-label";

describe("userLabel", () => {
  it("uses the nickname when there is one", () => {
    expect(userLabel(26, "devGuru")).toBe("devGuru");
  });

  it("falls back to the user id when the nickname is missing", () => {
    expect(userLabel(26, null)).toBe("사용자 #26");
    expect(userLabel(26)).toBe("사용자 #26");
  });
});
