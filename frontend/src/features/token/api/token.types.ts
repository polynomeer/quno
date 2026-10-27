/** Mirrors backend TokenResponse (Phase 65.1, ADR-0079). */
export type TokenScope = "READ" | "ASK";

export interface PersonalAccessToken {
  id: number;
  name: string;
  /** First characters of the secret, e.g. `quno_pat_ab1` — enough to tell tokens apart. */
  displayPrefix: string;
  scopes: TokenScope[];
  createdAt: string;
  expiresAt: string;
  lastUsedAt: string | null;
  revokedAt: string | null;
}

/** `secret` is in this response and nowhere else. */
export interface IssuedToken {
  secret: string;
  token: PersonalAccessToken;
}

export interface IssueTokenInput {
  name: string;
  scopes: TokenScope[];
  lifetimeDays: 30 | 90 | 365;
}
