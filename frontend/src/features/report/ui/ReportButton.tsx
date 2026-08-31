"use client";

import { useState } from "react";
import { useFileReport } from "../hooks/useFileReport";
import { Button } from "@/shared/ui/Button";
import { Textarea } from "@/shared/ui/Textarea";
import { ApiError } from "@/shared/api/api-error";
import type { ReportReason, ReportTargetType } from "../api/report.types";

const reasonLabels: Record<ReportReason, string> = {
  SPAM: "스팸",
  DUPLICATE: "중복 질문",
  LOW_QUALITY: "품질 낮음",
  OTHER: "기타",
};

/** No self-report restriction on the backend, so this doesn't check authorship either — anyone,
 * including the author, can report. Re-reporting isn't blocked client-side (backend doesn't
 * dedupe), just discouraged via a per-view "Reported" state after a successful submit. */
export function ReportButton({ targetType, targetId }: { targetType: ReportTargetType; targetId: number }) {
  const fileReport = useFileReport(targetType, targetId);
  const [isOpen, setIsOpen] = useState(false);
  const [reason, setReason] = useState<ReportReason>("SPAM");
  const [message, setMessage] = useState("");

  if (fileReport.isSuccess) {
    return <span className="text-xs text-text-secondary">Reported</span>;
  }

  if (!isOpen) {
    return (
      <button type="button" onClick={() => setIsOpen(true)} className="text-xs text-text-secondary hover:text-danger">
        Report
      </button>
    );
  }

  return (
    <div className="space-y-1.5 rounded-md border border-border p-2 text-xs">
      <select
        value={reason}
        onChange={(event) => setReason(event.target.value as ReportReason)}
        className="rounded-md border border-border bg-surface px-2 py-1 text-xs"
      >
        {Object.entries(reasonLabels).map(([value, label]) => (
          <option key={value} value={value}>
            {label}
          </option>
        ))}
      </select>
      <Textarea
        value={message}
        onChange={(event) => setMessage(event.target.value)}
        rows={2}
        placeholder="추가 설명 (선택)"
        className="text-xs"
      />
      {fileReport.isError && (
        <p className="text-danger">
          {fileReport.error instanceof ApiError ? fileReport.error.message : "신고를 접수하지 못했습니다."}
        </p>
      )}
      <div className="flex items-center gap-2">
        <Button
          variant="secondary"
          className="px-2 py-1 text-xs"
          onClick={() => fileReport.mutate({ reason, message: message.trim() || undefined })}
          disabled={fileReport.isPending}
        >
          {fileReport.isPending ? "접수 중..." : "신고"}
        </Button>
        <button type="button" onClick={() => setIsOpen(false)} className="text-text-secondary hover:underline">
          취소
        </button>
      </div>
    </div>
  );
}
