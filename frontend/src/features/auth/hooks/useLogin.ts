"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { authApi } from "@/features/auth/api/auth.api";
import { sessionQueryKey } from "@/features/auth/hooks/useSession";
import { tokenStorage } from "@/shared/lib/token-storage";
import type { LoginInput } from "@/features/auth/api/auth.types";

export function useLogin() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (input: LoginInput) => authApi.login(input),
    onSuccess: (tokens) => {
      tokenStorage.setTokens(tokens.accessToken, tokens.refreshToken);
      queryClient.invalidateQueries({ queryKey: sessionQueryKey });
    },
  });
}

export function useLogout() {
  const queryClient = useQueryClient();

  return () => {
    tokenStorage.clear();
    queryClient.setQueryData(sessionQueryKey, null);
  };
}
