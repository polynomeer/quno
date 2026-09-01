"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { authApi } from "@/features/auth/api/auth.api";
import { sessionQueryKey } from "@/features/auth/hooks/useSession";

export function useUpdateDirectAskSettings() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (accepts: boolean) => authApi.updateDirectAskSettings(accepts),
    onSuccess: (myProfile) => {
      queryClient.setQueryData(sessionQueryKey, myProfile);
    },
  });
}
