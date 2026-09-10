"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { authApi } from "@/features/auth/api/auth.api";
import { sessionQueryKey } from "@/features/auth/hooks/useSession";
import { tokenStorage } from "@/shared/lib/token-storage";

export function useWithdrawAccount() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (password: string) => authApi.withdraw(password),
    onSuccess: () => {
      tokenStorage.clear();
      queryClient.setQueryData(sessionQueryKey, null);
    },
  });
}
