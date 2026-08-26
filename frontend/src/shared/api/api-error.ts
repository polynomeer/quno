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
