"use client";

import { useQuery } from "@tanstack/react-query";
import { dashboardApi } from "../api/dashboard.api";
import { dashboardKeys } from "../api/dashboard.keys";

export function useFlow(enabled: boolean) {
  return useQuery({
    queryKey: dashboardKeys.flow,
    queryFn: () => dashboardApi.flow(),
    enabled,
  });
}
