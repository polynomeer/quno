import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { httpClient } from "./http-client";
import { ApiError, RequestTimeoutError } from "./api-error";
import { tokenStorage } from "@/shared/lib/token-storage";

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

describe("httpClient", () => {
  beforeEach(() => {
    tokenStorage.clear();
    vi.stubGlobal("fetch", vi.fn());
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("attaches the Authorization header when an access token is present", async () => {
    tokenStorage.setTokens("access-1", "refresh-1");
    vi.mocked(fetch).mockResolvedValueOnce(jsonResponse(200, { ok: true }));

    await httpClient.get("/api/v1/me");

    const [, init] = vi.mocked(fetch).mock.calls[0];
    expect((init?.headers as Record<string, string>).Authorization).toBe("Bearer access-1");
  });

  it("skips the Authorization header when skipAuth is set", async () => {
    tokenStorage.setTokens("access-1", "refresh-1");
    vi.mocked(fetch).mockResolvedValueOnce(jsonResponse(200, { ok: true }));

    await httpClient.post("/api/v1/auth/login", { email: "a@b.com" }, { skipAuth: true });

    const [, init] = vi.mocked(fetch).mock.calls[0];
    expect((init?.headers as Record<string, string>).Authorization).toBeUndefined();
  });

  it("throws an ApiError with the backend's code/message on failure", async () => {
    vi.mocked(fetch).mockResolvedValueOnce(jsonResponse(404, { code: "NOT_FOUND", message: "Question not found" }));

    await expect(httpClient.get("/api/v1/questions/999")).rejects.toMatchObject({
      status: 404,
      code: "NOT_FOUND",
      message: "Question not found",
    } satisfies Partial<ApiError>);
  });

  it("refreshes the access token once on a 401 and retries the original request", async () => {
    tokenStorage.setTokens("expired-access", "refresh-1");
    vi.mocked(fetch)
      .mockResolvedValueOnce(jsonResponse(401, { code: "UNAUTHORIZED", message: "expired" })) // original request
      .mockResolvedValueOnce(jsonResponse(200, { accessToken: "new-access", refreshToken: "new-refresh" })) // refresh call
      .mockResolvedValueOnce(jsonResponse(200, { ok: true })); // retried request

    const result = await httpClient.get<{ ok: boolean }>("/api/v1/me");

    expect(result).toEqual({ ok: true });
    expect(tokenStorage.getAccessToken()).toBe("new-access");
    expect(fetch).toHaveBeenCalledTimes(3);
    const [, retryInit] = vi.mocked(fetch).mock.calls[2];
    expect((retryInit?.headers as Record<string, string>).Authorization).toBe("Bearer new-access");
  });

  it("clears tokens and throws when the refresh call itself fails", async () => {
    tokenStorage.setTokens("expired-access", "refresh-1");
    vi.mocked(fetch)
      .mockResolvedValueOnce(jsonResponse(401, { code: "UNAUTHORIZED", message: "expired" }))
      .mockResolvedValueOnce(jsonResponse(401, { code: "UNAUTHORIZED", message: "refresh expired too" }));

    await expect(httpClient.get("/api/v1/me")).rejects.toBeInstanceOf(ApiError);
    expect(tokenStorage.getAccessToken()).toBeNull();
  });

  it("returns undefined for a 204 No Content response", async () => {
    tokenStorage.setTokens("access-1", "refresh-1");
    vi.mocked(fetch).mockResolvedValueOnce(new Response(null, { status: 204 }));

    const result = await httpClient.delete("/api/v1/me");

    expect(result).toBeUndefined();
  });

  it("passes a default AbortSignal to fetch so a hung dependency can't hang the UI forever (see ADR-0058)", async () => {
    vi.mocked(fetch).mockResolvedValueOnce(jsonResponse(200, { ok: true }));

    await httpClient.get("/api/v1/me");

    const [, init] = vi.mocked(fetch).mock.calls[0];
    expect(init?.signal).toBeInstanceOf(AbortSignal);
  });

  it("surfaces a client-side timeout as a distinguishable RequestTimeoutError, not an ApiError", async () => {
    // AbortSignal.timeout() rejects fetch with a DOMException named "TimeoutError" — simulate
    // that directly rather than waiting out the real 15s default timeout.
    vi.mocked(fetch).mockRejectedValueOnce(new DOMException("The operation timed out.", "TimeoutError"));

    const promise = httpClient.get("/api/v1/questions/1");

    await expect(promise).rejects.toBeInstanceOf(RequestTimeoutError);
    await expect(promise).rejects.not.toBeInstanceOf(ApiError);
  });

  it("re-throws non-timeout fetch failures (e.g. the backend is fully unreachable) unchanged", async () => {
    const networkError = new TypeError("Failed to fetch");
    vi.mocked(fetch).mockRejectedValueOnce(networkError);

    await expect(httpClient.get("/api/v1/me")).rejects.toBe(networkError);
  });
});
