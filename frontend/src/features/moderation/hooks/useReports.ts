"use client";

import { useQuery } from "@tanstack/react-query";
import { moderationApi } from "../api/moderation.api";
import { moderationKeys } from "../api/moderation.keys";
import type { ReportStatus } from "@/features/report/api/report.types";

export function useReports(status: ReportStatus) {
  return useQuery({
    queryKey: moderationKeys.reports(status),
    queryFn: () => moderationApi.listReports(status),
    retry: false,
  });
}
