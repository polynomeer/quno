"use client";

import { useQuery } from "@tanstack/react-query";
import { dashboardApi } from "../api/dashboard.api";
import { dashboardKeys } from "../api/dashboard.keys";

export function useDashboard(enabled: boolean) {
  return useQuery({
    queryKey: dashboardKeys.dashboard,
    queryFn: dashboardApi.get,
    enabled,
  });
}
