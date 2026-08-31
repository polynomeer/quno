"use client";

import { useQuery } from "@tanstack/react-query";
import { followApi } from "../api/follow.api";
import { followKeys } from "../api/follow.keys";

export function useMyFollowing(enabled: boolean) {
  return useQuery({
    queryKey: followKeys.mine,
    queryFn: followApi.myFollowing,
    enabled,
  });
}
