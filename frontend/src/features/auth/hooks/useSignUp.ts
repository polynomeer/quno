"use client";

import { useMutation } from "@tanstack/react-query";
import { authApi } from "@/features/auth/api/auth.api";
import { useLogin } from "@/features/auth/hooks/useLogin";
import type { SignUpInput } from "@/features/auth/api/auth.types";

/** 회원가입 직후 바로 로그인까지 이어간다 — 별도 로그인 화면으로 다시 보내는 것보다 자연스럽다.
 * 토큰 저장 등 로그인 성공 시 부수효과는 useLogin이 이미 갖고 있어 그대로 재사용한다. */
export function useSignUp() {
  const login = useLogin();

  return useMutation({
    mutationFn: async (input: SignUpInput) => {
      await authApi.signUp(input);
      return login.mutateAsync({ email: input.email, password: input.password });
    },
  });
}
