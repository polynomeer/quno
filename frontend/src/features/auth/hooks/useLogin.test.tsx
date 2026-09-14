import { beforeEach, describe, expect, it, vi } from "vitest";
import { renderHook, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import type { ReactNode } from "react";
import { useLogin, useLogout } from "./useLogin";
import { sessionQueryKey } from "./useSession";
import { authApi } from "../api/auth.api";
import { tokenStorage } from "@/shared/lib/token-storage";
import type { MyProfile, TokenResponse } from "../api/auth.types";

vi.mock("../api/auth.api", () => ({
  authApi: { login: vi.fn(), me: vi.fn() },
}));

const TOKENS: TokenResponse = { accessToken: "access-token", refreshToken: "refresh-token" };
const ME: MyProfile = { id: 1, email: "me@example.com", nickname: "me", acceptsDirectAsk: false, createdAt: "" };

function wrapper(queryClient: QueryClient) {
  return function Wrapper({ children }: { children: ReactNode }) {
    return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
  };
}

describe("useLogin", () => {
  beforeEach(() => {
    vi.mocked(authApi.login).mockReset();
    tokenStorage.clear();
  });

  it("stores the returned tokens on success", async () => {
    vi.mocked(authApi.login).mockResolvedValue(TOKENS);
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    const { result } = renderHook(() => useLogin(), { wrapper: wrapper(queryClient) });

    result.current.mutate({ email: "me@example.com", password: "password123" });

    await waitFor(() => expect(tokenStorage.getAccessToken()).toBe("access-token"));
    expect(tokenStorage.getRefreshToken()).toBe("refresh-token");
  });

  it("invalidates the session query on success so /me is refetched", async () => {
    vi.mocked(authApi.login).mockResolvedValue(TOKENS);
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    const invalidateQueries = vi.spyOn(queryClient, "invalidateQueries");
    const { result } = renderHook(() => useLogin(), { wrapper: wrapper(queryClient) });

    result.current.mutate({ email: "me@example.com", password: "password123" });

    await waitFor(() => expect(invalidateQueries).toHaveBeenCalledWith({ queryKey: sessionQueryKey }));
  });

  it("does not store tokens when login fails", async () => {
    vi.mocked(authApi.login).mockRejectedValue(new Error("invalid credentials"));
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    const { result } = renderHook(() => useLogin(), { wrapper: wrapper(queryClient) });

    result.current.mutate({ email: "me@example.com", password: "wrong" });

    await waitFor(() => expect(result.current.isError).toBe(true));
    expect(tokenStorage.getAccessToken()).toBeNull();
  });
});

describe("useLogout", () => {
  beforeEach(() => {
    tokenStorage.clear();
  });

  it("clears stored tokens and resets the cached session to null", () => {
    tokenStorage.setTokens("access-token", "refresh-token");
    const queryClient = new QueryClient();
    queryClient.setQueryData(sessionQueryKey, ME);
    const { result } = renderHook(() => useLogout(), { wrapper: wrapper(queryClient) });

    result.current();

    expect(tokenStorage.getAccessToken()).toBeNull();
    expect(queryClient.getQueryData(sessionQueryKey)).toBeNull();
  });
});
