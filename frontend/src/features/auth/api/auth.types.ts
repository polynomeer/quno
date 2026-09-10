/** Mirrors backend/.../interfaces/api/user (AuthResponses.kt, UserProfileResponse). */

export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
}

export interface MyProfile {
  id: number;
  email: string;
  nickname: string;
  acceptsDirectAsk: boolean;
  createdAt: string;
}

export interface LoginInput {
  email: string;
  password: string;
}

export interface SignUpInput {
  email: string;
  nickname: string;
  password: string;
}

/** Mirrors MyDataExportResult (ADR-0046) — profile + authored content only. */
export interface MyDataExport {
  userId: number;
  email: string;
  nickname: string;
  createdAt: string;
  questions: Array<{ id: number; title: string; createdAt: string }>;
  answers: Array<{ id: number; questionId: number; createdAt: string }>;
}
