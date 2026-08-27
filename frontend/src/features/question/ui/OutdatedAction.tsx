"use client";

import { useState } from "react";
import { useMarkOutdated } from "../hooks/useMarkOutdated";
import { Input } from "@/shared/ui/Input";
import { Button } from "@/shared/ui/Button";
import { ApiError } from "@/shared/api/api-error";
import type { QuestionStatus } from "@/shared/ui/StatusBadge";

/** No permission restriction on the backend — anyone, including the author, can mark a question
 * outdated (community-judgment model, same as Cluster — see ADR-0017). */
export function OutdatedAction({ questionId, status }: { questionId: number; status: QuestionStatus }) {
  const [open, setOpen] = useState(false);
  const [reason, setReason] = useState("");
  const markOutdated = useMarkOutdated(questionId);

  if (status === "OUTDATED") {
    return null;
  }

  if (!open) {
    return (
      <Button variant="ghost" className="text-text-secondary" onClick={() => setOpen(true)}>
        Outdated로 표시
      </Button>
    );
  }

  function handleSubmit() {
    if (!reason.trim()) return;
    markOutdated.mutate(reason.trim(), { onSuccess: () => setOpen(false) });
  }

  return (
    <div className="flex flex-wrap items-center gap-2">
      <Input
        value={reason}
        onChange={(event) => setReason(event.target.value)}
        placeholder="Outdated 사유 (예: Spring Boot 4에서 더 이상 재현되지 않음)"
        className="max-w-sm"
      />
      <Button variant="danger" onClick={handleSubmit} disabled={markOutdated.isPending || !reason.trim()}>
        {markOutdated.isPending ? "표시 중..." : "표시"}
      </Button>
      <Button variant="ghost" onClick={() => setOpen(false)}>
        취소
      </Button>
      {markOutdated.isError && (
        <p className="w-full text-sm text-danger">
          {markOutdated.error instanceof ApiError ? markOutdated.error.message : "표시하지 못했습니다."}
        </p>
      )}
    </div>
  );
}
