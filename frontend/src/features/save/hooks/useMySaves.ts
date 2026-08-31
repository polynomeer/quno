"use client";

import { useQuery } from "@tanstack/react-query";
import { saveApi } from "../api/save.api";
import { saveKeys } from "../api/save.keys";

export function useMySaves(enabled: boolean) {
  return useQuery({
    queryKey: saveKeys.mine,
    queryFn: saveApi.mySaves,
    enabled,
  });
}
