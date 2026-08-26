import { httpClient } from "@/shared/api/http-client";
import type { LoginInput, MyProfile, SignUpInput, TokenResponse } from "./auth.types";

export const authApi = {
  signUp: (input: SignUpInput) =>
    httpClient.post<{ id: number; email: string; nickname: string }>("/api/v1/auth/signup", input, {
      skipAuth: true,
    }),
  login: (input: LoginInput) => httpClient.post<TokenResponse>("/api/v1/auth/login", input, { skipAuth: true }),
  me: () => httpClient.get<MyProfile>("/api/v1/me"),
};
