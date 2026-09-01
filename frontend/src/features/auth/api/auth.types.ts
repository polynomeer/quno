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
