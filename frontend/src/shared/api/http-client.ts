import { ApiError, RequestTimeoutError } from "./api-error";
import { tokenStorage } from "@/shared/lib/token-storage";

/** Base URL of the Kotlin Spring Boot API (see docs/architecture/system-architecture.md). */
const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8081";

/**
 * Default client-side request timeout (see ADR-0058). The backend's own dependency timeouts
 * (ADR-0054 HikariCP, ADR-0055 Mongo) are 3s each, so a healthy backend always responds well
 * under this; it exists so the UI can't hang forever if a future path isn't covered by those
 * bounded timeouts.
 */
const DEFAULT_TIMEOUT_MS = 15_000;

interface RequestOptions extends Omit<RequestInit, "body"> {
  body?: unknown;
  /** Skip attaching the Authorization header — used by signup/login/refresh themselves. */
  skipAuth?: boolean;
}

interface TokenResponse {
  accessToken: string;
  refreshToken: string;
}

// Multiple requests can 401 at once; share one in-flight refresh instead of racing.
let refreshPromise: Promise<void> | null = null;

async function refreshAccessToken(): Promise<void> {
  const refreshToken = tokenStorage.getRefreshToken();
  if (!refreshToken) {
    throw new ApiError(401, "UNAUTHORIZED", "No refresh token available");
  }

  const response = await fetch(`${API_BASE_URL}/api/v1/auth/refresh`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ refreshToken }),
  });

  if (!response.ok) {
    tokenStorage.clear();
    throw new ApiError(response.status, "UNAUTHORIZED", "Session expired");
  }

  const data = (await response.json()) as TokenResponse;
  tokenStorage.setTokens(data.accessToken, data.refreshToken);
}

function getOrCreateRefresh(): Promise<void> {
  if (!refreshPromise) {
    refreshPromise = refreshAccessToken().finally(() => {
      refreshPromise = null;
    });
  }
  return refreshPromise;
}

async function request<T>(path: string, options: RequestOptions = {}, isRetry = false): Promise<T> {
  const { body, skipAuth, headers, signal, ...rest } = options;
  const accessToken = skipAuth ? null : tokenStorage.getAccessToken();

  let response: Response;
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      ...rest,
      headers: {
        "Content-Type": "application/json",
        ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
        ...headers,
      },
      body: body !== undefined ? JSON.stringify(body) : undefined,
      signal: signal ?? AbortSignal.timeout(DEFAULT_TIMEOUT_MS),
    });
  } catch (error) {
    if (error instanceof DOMException && error.name === "TimeoutError") {
      throw new RequestTimeoutError(`Request to ${path} timed out after ${DEFAULT_TIMEOUT_MS}ms`);
    }
    throw error;
  }

  if (response.status === 204) {
    return undefined as T;
  }

  const text = await response.text();
  const data: unknown = text ? JSON.parse(text) : undefined;

  if (!response.ok) {
    if (response.status === 401 && !skipAuth && !isRetry && tokenStorage.getRefreshToken()) {
      await getOrCreateRefresh();
      return request<T>(path, options, true);
    }
    const errorBody = data as { code?: string; message?: string } | undefined;
    throw new ApiError(response.status, errorBody?.code ?? "UNKNOWN", errorBody?.message ?? response.statusText);
  }

  return data as T;
}

export const httpClient = {
  get: <T>(path: string, options?: RequestOptions) => request<T>(path, { ...options, method: "GET" }),
  post: <T>(path: string, body?: unknown, options?: RequestOptions) =>
    request<T>(path, { ...options, method: "POST", body }),
  put: <T>(path: string, body?: unknown, options?: RequestOptions) =>
    request<T>(path, { ...options, method: "PUT", body }),
  delete: <T>(path: string, options?: RequestOptions) => request<T>(path, { ...options, method: "DELETE" }),
};
