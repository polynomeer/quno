"use client";

import Link from "next/link";
import { useDismissReport } from "../hooks/useDismissReport";
import { useHideReport } from "../hooks/useHideReport";
import { Button } from "@/shared/ui/Button";
import { relativeTime } from "@/shared/lib/relative-time";
import type { Report, ReportReason } from "@/features/report/api/report.types";

const reasonLabels: Record<ReportReason, string> = {
  SPAM: "스팸",
  DUPLICATE: "중복 질문",
  LOW_QUALITY: "품질 낮음",
  OTHER: "기타",
};

export function ReportQueueItem({ report }: { report: Report }) {
  const dismissReport = useDismissReport();
  const hideReport = useHideReport();
  const isPending = dismissReport.isPending || hideReport.isPending;

  return (
    <li className="space-y-2 px-4 py-4 transition-colors hover:bg-canvas/60 sm:px-5">
      <div className="flex flex-wrap items-center gap-2 text-[13px] text-text-secondary">
        <span className="inline-flex h-[22px] items-center rounded bg-warning-subtle px-2 text-xs font-semibold text-warning">
          {reasonLabels[report.reason]}
        </span>
        {report.targetType === "QUESTION" ? (
          <Link href={`/questions/${report.targetId}`} className="font-mono font-medium text-text-primary underline hover:text-brand">
            Question #{report.targetId}
          </Link>
        ) : (
          // No single-answer-by-id page exists yet (the backend doesn't return the parent
          // question id from a bare answer lookup), so this can't deep-link like Question does.
          <span className="font-mono">Answer #{report.targetId}</span>
        )}
        <span>
          · 신고자 <span className="font-mono">#{report.reporterId}</span>
        </span>
        <span>· {relativeTime(report.createdAt)}</span>
      </div>
      {report.message && <p className="text-sm text-text-body">{report.message}</p>}
      <div className="flex gap-2">
        <Button
          variant="secondary"
          className="px-2 py-1 text-xs"
          onClick={() => dismissReport.mutate(report.id)}
          disabled={isPending}
        >
          {dismissReport.isPending ? "처리 중..." : "Keep (Dismiss)"}
        </Button>
        <Button
          variant="danger"
          className="px-2 py-1 text-xs"
          onClick={() => hideReport.mutate(report.id)}
          disabled={isPending}
        >
          {hideReport.isPending ? "처리 중..." : "Hide"}
        </Button>
      </div>
    </li>
  );
}
