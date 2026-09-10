import { beforeEach, describe, expect, it } from "vitest";
import { tokenStorage } from "./token-storage";

describe("tokenStorage", () => {
  beforeEach(() => {
    tokenStorage.clear();
  });

  it("has no tokens initially", () => {
    expect(tokenStorage.getAccessToken()).toBeNull();
    expect(tokenStorage.getRefreshToken()).toBeNull();
  });

  it("round-trips both tokens", () => {
    tokenStorage.setTokens("access-1", "refresh-1");

    expect(tokenStorage.getAccessToken()).toBe("access-1");
    expect(tokenStorage.getRefreshToken()).toBe("refresh-1");
  });

  it("clears both tokens", () => {
    tokenStorage.setTokens("access-1", "refresh-1");

    tokenStorage.clear();

    expect(tokenStorage.getAccessToken()).toBeNull();
    expect(tokenStorage.getRefreshToken()).toBeNull();
  });
});
