"use client";

import { useQuery } from "@tanstack/react-query";
import { badgeApi } from "../api/badge.api";
import { badgeKeys } from "../api/badge.keys";

export function useBadges(userId: number) {
  return useQuery({
    queryKey: badgeKeys.list(userId),
    queryFn: () => badgeApi.list(userId),
  });
}
