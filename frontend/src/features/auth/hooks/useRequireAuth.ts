"use client";

import { useEffect } from "react";
import { usePathname, useRouter } from "next/navigation";
import { tokenStorage } from "@/shared/lib/token-storage";
import { useSession } from "./useSession";

/**
 * Every read page currently requires auth on the backend (ADR-0013), so pages with no
 * meaningful guest fallback redirect straight to /login, preserving where to come back to.
 *
 * The redirect decision never relies on `isLoading` alone: right after an enabled query flips
 * on, there's a real render where `isLoading` is still false simply because fetching hasn't
 * started yet — indistinguishable from "confirmed guest" if that's all we check, which fires a
 * false redirect for an already-logged-in user. Reading the token directly is safe here (unlike
 * during render) because effects only run in the browser, never during SSR. No token → redirect
 * immediately; a token exists → wait for the /me request to actually settle (`isFetched`) before
 * treating an empty result as logged out (e.g. an expired refresh token).
 */
export function useRequireAuth() {
  const { data: me, isFetched, isLoading } = useSession();
  const router = useRouter();
  const pathname = usePathname();

  useEffect(() => {
    const hasToken = Boolean(tokenStorage.getAccessToken());
    if (!hasToken || (isFetched && !me)) {
      router.replace(`/login?redirectTo=${encodeURIComponent(pathname)}`);
    }
  }, [isFetched, me, pathname, router]);

  return { me, isLoading: isLoading || !me };
}
