import { httpClient } from "@/shared/api/http-client";
import type { LoginInput, MyDataExport, MyProfile, SignUpInput, TokenResponse } from "./auth.types";

export const authApi = {
  signUp: (input: SignUpInput) =>
    httpClient.post<{ id: number; email: string; nickname: string }>("/api/v1/auth/signup", input, {
      skipAuth: true,
    }),
  login: (input: LoginInput) => httpClient.post<TokenResponse>("/api/v1/auth/login", input, { skipAuth: true }),
  me: () => httpClient.get<MyProfile>("/api/v1/me"),
  updateDirectAskSettings: (accepts: boolean) => httpClient.put<MyProfile>("/api/v1/me/direct-ask-settings", { accepts }),
  /** 계정 row는 유지되고 PII만 익명화된다(ADR-0046) — 탈취된 access token만으로 탈퇴시키는 것을
   * 막기 위해 백엔드가 현재 비밀번호 재확인을 요구한다. */
  withdraw: (password: string) => httpClient.delete<void>("/api/v1/me", { body: { password } }),
  exportMyData: () => httpClient.get<MyDataExport>("/api/v1/me/data-export"),
};
