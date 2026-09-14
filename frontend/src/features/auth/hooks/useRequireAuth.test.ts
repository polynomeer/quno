import { beforeEach, describe, expect, it, vi } from "vitest";
import { renderHook } from "@testing-library/react";
import { useRequireAuth } from "./useRequireAuth";
import { useSession } from "./useSession";
import { tokenStorage } from "@/shared/lib/token-storage";
import type { MyProfile } from "../api/auth.types";

const replace = vi.fn();

vi.mock("next/navigation", () => ({
  useRouter: () => ({ replace }),
  usePathname: () => "/questions/10",
}));

vi.mock("./useSession", () => ({ useSession: vi.fn() }));

const ME: MyProfile = { id: 1, email: "me@example.com", nickname: "me", acceptsDirectAsk: false, createdAt: "" };

function mockSession(overrides: Partial<ReturnType<typeof useSession>>) {
  vi.mocked(useSession).mockReturnValue({
    data: undefined,
    isFetched: false,
    isLoading: true,
    ...overrides,
  } as ReturnType<typeof useSession>);
}

describe("useRequireAuth", () => {
  beforeEach(() => {
    replace.mockReset();
    tokenStorage.clear();
  });

  it("redirects to /login with the current path when there is no access token", () => {
    mockSession({ isFetched: false, isLoading: false, data: undefined });

    renderHook(() => useRequireAuth());

    expect(replace).toHaveBeenCalledWith("/login?redirectTo=%2Fquestions%2F10");
  });

  it("does not redirect while a token exists and the session hasn't been fetched yet", () => {
    tokenStorage.setTokens("access", "refresh");
    mockSession({ isFetched: false, isLoading: true, data: undefined });

    renderHook(() => useRequireAuth());

    expect(replace).not.toHaveBeenCalled();
  });

  it("does not redirect once the session is fetched and resolves to a real user", () => {
    tokenStorage.setTokens("access", "refresh");
    mockSession({ isFetched: true, isLoading: false, data: ME });

    renderHook(() => useRequireAuth());

    expect(replace).not.toHaveBeenCalled();
  });

  it("redirects once the session is fetched but comes back empty (e.g. expired refresh token)", () => {
    tokenStorage.setTokens("access", "refresh");
    mockSession({ isFetched: true, isLoading: false, data: undefined });

    renderHook(() => useRequireAuth());

    expect(replace).toHaveBeenCalledWith("/login?redirectTo=%2Fquestions%2F10");
  });

  it("reports isLoading true until the user is actually known", () => {
    tokenStorage.setTokens("access", "refresh");
    mockSession({ isFetched: false, isLoading: false, data: undefined });

    const { result } = renderHook(() => useRequireAuth());

    expect(result.current.isLoading).toBe(true);
  });

  it("reports isLoading false and returns the user once resolved", () => {
    tokenStorage.setTokens("access", "refresh");
    mockSession({ isFetched: true, isLoading: false, data: ME });

    const { result } = renderHook(() => useRequireAuth());

    expect(result.current.isLoading).toBe(false);
    expect(result.current.me).toEqual(ME);
  });
});
