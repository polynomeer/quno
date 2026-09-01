"use client";

import { useQuery } from "@tanstack/react-query";
import { userApi } from "../api/user.api";
import { userKeys } from "../api/user.keys";

export function useUserProfile(id: number, enabled = true) {
  return useQuery({
    queryKey: userKeys.profile(id),
    queryFn: () => userApi.getProfile(id),
    enabled,
  });
}
