"use client";

import { useQuery } from "@tanstack/react-query";
import { authApi } from "@/features/auth/api/auth.api";
import { tokenStorage } from "@/shared/lib/token-storage";

export const sessionQueryKey = ["session", "me"] as const;

/** Only fetches `GET /me` when a token is actually present, to avoid a guaranteed 401 for guests. */
export function useSession() {
  const hasToken = typeof window !== "undefined" && Boolean(tokenStorage.getAccessToken());

  return useQuery({
    queryKey: sessionQueryKey,
    queryFn: authApi.me,
    enabled: hasToken,
    retry: false,
  });
}
