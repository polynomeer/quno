import type { ReactNode } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { renderHook, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { useSignUp } from "./useSignUp";
import { authApi } from "../api/auth.api";
import { tokenStorage } from "@/shared/lib/token-storage";
import type { TokenResponse } from "../api/auth.types";

vi.mock("../api/auth.api", () => ({
  authApi: { signUp: vi.fn(), login: vi.fn() },
}));

const TOKENS: TokenResponse = { accessToken: "access-token", refreshToken: "refresh-token" };
const SIGNUP_INPUT = { email: "new@example.com", nickname: "newbie", password: "password123" };

function wrapper({ children }: { children: ReactNode }) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
}

describe("useSignUp", () => {
  beforeEach(() => {
    vi.mocked(authApi.signUp).mockReset();
    vi.mocked(authApi.login).mockReset();
    tokenStorage.clear();
  });

  it("signs up then logs in with the same credentials, storing the resulting tokens", async () => {
    vi.mocked(authApi.signUp).mockResolvedValue({ id: 1, email: SIGNUP_INPUT.email, nickname: SIGNUP_INPUT.nickname });
    vi.mocked(authApi.login).mockResolvedValue(TOKENS);
    const { result } = renderHook(() => useSignUp(), { wrapper });

    result.current.mutate(SIGNUP_INPUT);

    await waitFor(() => expect(tokenStorage.getAccessToken()).toBe("access-token"));
    expect(authApi.signUp).toHaveBeenCalledWith(SIGNUP_INPUT);
    expect(authApi.login).toHaveBeenCalledWith({ email: SIGNUP_INPUT.email, password: SIGNUP_INPUT.password });
  });

  it("does not attempt to log in when the signup call itself fails", async () => {
    vi.mocked(authApi.signUp).mockRejectedValue(new Error("이메일이 이미 사용 중입니다"));
    const { result } = renderHook(() => useSignUp(), { wrapper });

    result.current.mutate(SIGNUP_INPUT);

    await waitFor(() => expect(result.current.isError).toBe(true));
    expect(authApi.login).not.toHaveBeenCalled();
    expect(tokenStorage.getAccessToken()).toBeNull();
  });

  it("surfaces an error if signup succeeds but the follow-up login fails", async () => {
    vi.mocked(authApi.signUp).mockResolvedValue({ id: 1, email: SIGNUP_INPUT.email, nickname: SIGNUP_INPUT.nickname });
    vi.mocked(authApi.login).mockRejectedValue(new Error("로그인 실패"));
    const { result } = renderHook(() => useSignUp(), { wrapper });

    result.current.mutate(SIGNUP_INPUT);

    await waitFor(() => expect(result.current.isError).toBe(true));
    expect(tokenStorage.getAccessToken()).toBeNull();
  });
});
