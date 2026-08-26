/**
 * JWT Access/Refresh Token persistence (see docs/architecture/decisions/0003-stateless-jwt-auth.md).
 * Lives in shared/ (not features/auth) so shared/api's http-client can read it without a
 * shared → feature dependency — see docs/frontend/architecture.md #26 디렉터리·코드 구조.
 */
const ACCESS_TOKEN_KEY = "quno.accessToken";
const REFRESH_TOKEN_KEY = "quno.refreshToken";

function isBrowser(): boolean {
  return typeof window !== "undefined";
}

export const tokenStorage = {
  getAccessToken(): string | null {
    return isBrowser() ? localStorage.getItem(ACCESS_TOKEN_KEY) : null;
  },
  getRefreshToken(): string | null {
    return isBrowser() ? localStorage.getItem(REFRESH_TOKEN_KEY) : null;
  },
  setTokens(accessToken: string, refreshToken: string): void {
    if (!isBrowser()) return;
    localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
    localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
  },
  clear(): void {
    if (!isBrowser()) return;
    localStorage.removeItem(ACCESS_TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
  },
};
