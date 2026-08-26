"use client";

import { useSyncExternalStore } from "react";
import { useQuery } from "@tanstack/react-query";
import { authApi } from "@/features/auth/api/auth.api";
import { tokenStorage } from "@/shared/lib/token-storage";

export const sessionQueryKey = ["session", "me"] as const;

function subscribe(callback: () => void) {
  window.addEventListener("storage", callback);
  return () => window.removeEventListener("storage", callback);
}

function getSnapshot() {
  return Boolean(tokenStorage.getAccessToken());
}

function getServerSnapshot() {
  return false;
}

/**
 * Only fetches `GET /me` when a token is actually present, to avoid a guaranteed 401 for guests.
 * `useSyncExternalStore` (not useState+useEffect) is what makes this hydration-safe: the server
 * snapshot is always `false` (no localStorage on the server), and React resolves the real client
 * value with its own corrective re-render during hydration instead of us staging it through an
 * effect — that staging is what caused a visible AppHeader login/logout hydration mismatch.
 */
export function useSession() {
  const hasToken = useSyncExternalStore(subscribe, getSnapshot, getServerSnapshot);

  return useQuery({
    queryKey: sessionQueryKey,
    queryFn: authApi.me,
    enabled: hasToken,
    retry: false,
  });
}
