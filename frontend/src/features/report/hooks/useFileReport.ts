"use client";

import { useMutation } from "@tanstack/react-query";
import { reportApi } from "../api/report.api";
import type { ReportReason, ReportTargetType } from "../api/report.types";

/** No cache to invalidate — reports don't change anything the viewer can see on this page
 * (backend doesn't expose "did I report this"), so there's nothing to refetch on success. */
export function useFileReport(targetType: ReportTargetType, targetId: number) {
  return useMutation({
    mutationFn: ({ reason, message }: { reason: ReportReason; message?: string }) =>
      targetType === "QUESTION"
        ? reportApi.reportQuestion(targetId, reason, message)
        : reportApi.reportAnswer(targetId, reason, message),
  });
}
