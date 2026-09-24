/**
 * Mirrors the backend's ErrorResponse{code, message} shape (GlobalExceptionHandler,
 * see docs/architecture/api-design.md). `code` is more reliable to branch on than the raw HTTP
 * status — see docs/frontend/architecture.md #282 Response Handling.
 */
export class ApiError extends Error {
  constructor(
    public readonly status: number,
    public readonly code: string,
    message: string,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

/**
 * The client aborted the request itself (see `httpClient`'s default `AbortSignal.timeout`)
 * before any response arrived. Deliberately NOT an `ApiError` subclass — there is no HTTP
 * status/code from the server to carry, and callers that branch on `instanceof ApiError`
 * (e.g. to show a 404-specific message) must not treat a timeout as one of those cases.
 */
export class RequestTimeoutError extends Error {
  constructor(message: string) {
    super(message);
    this.name = "RequestTimeoutError";
  }
}
