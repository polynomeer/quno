"use client";

import { useQuery } from "@tanstack/react-query";
import { watchApi } from "../api/watch.api";
import { watchKeys } from "../api/watch.keys";

export function useMyWatches(enabled: boolean) {
  return useQuery({
    queryKey: watchKeys.mine,
    queryFn: watchApi.myWatches,
    enabled,
  });
}
