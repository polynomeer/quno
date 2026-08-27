"use client";

import { useState } from "react";
import { useReviewRequests } from "../hooks/useReviewRequests";
import { useCreateReviewRequest } from "../hooks/useCreateReviewRequest";
import { useReRequestReview } from "../hooks/useReRequestReview";
import { Textarea } from "@/shared/ui/Textarea";
import { Button } from "@/shared/ui/Button";
import { relativeTime } from "@/shared/lib/relative-time";
import { ApiError } from "@/shared/api/api-error";

/**
 * QPR (Question Progress Review) — this UI has no design.md precedent, it's built directly from
 * the backend's multi-reviewer thread model (ADR-0012, api-design.md §QPR Review).
 */
export function ReviewRequestPanel({
  questionId,
  questionAuthorId,
  questionVersionNumber,
  currentUserId,
}: {
  questionId: number;
  questionAuthorId: number;
  questionVersionNumber: number;
  currentUserId: number;
}) {
  const { data: requests } = useReviewRequests(questionId);
  const createRequest = useCreateReviewRequest(questionId);
  const reRequest = useReRequestReview(questionId);
  const [message, setMessage] = useState("");
  const isAuthor = currentUserId === questionAuthorId;

  async function handleCreate() {
    if (!message.trim()) return;
    try {
      await createRequest.mutateAsync(message.trim());
      setMessage("");
    } catch {
      // error surfaced below via createRequest.error
    }
  }

  if (!requests?.length && isAuthor) {
    return null;
  }

  return (
    <section className="space-y-3 rounded-lg border border-border p-4">
      <h2 className="text-sm font-semibold text-text-secondary">정보 요청 (QPR)</h2>

      {requests && requests.length > 0 && (
        <ul className="space-y-2">
          {requests.map((request) => {
            const canReRequest =
              isAuthor && request.status === "OPEN" && questionVersionNumber > request.questionVersionNumberAtRequest;
            return (
              <li key={request.id} className="rounded-md border border-border p-3 text-sm">
                <div className="flex flex-wrap items-center gap-2 text-xs text-text-secondary">
                  <span
                    className={
                      request.status === "OPEN"
                        ? "inline-flex items-center rounded-full bg-warning-subtle px-2 py-0.5 font-medium text-warning"
                        : "inline-flex items-center rounded-full bg-success-subtle px-2 py-0.5 font-medium text-success"
                    }
                  >
                    {request.status}
                  </span>
                  <span>사용자 #{request.requestedBy}</span>
                  <span>· v{request.questionVersionNumberAtRequest} 기준</span>
                  <span>· {relativeTime(request.createdAt)}</span>
                  {canReRequest && (
                    <Button
                      variant="secondary"
                      className="ml-auto px-2 py-1 text-xs"
                      onClick={() => reRequest.mutate(request.id)}
                      disabled={reRequest.isPending}
                    >
                      재요청 처리
                    </Button>
                  )}
                </div>
                <p className="mt-2">{request.message}</p>
              </li>
            );
          })}
        </ul>
      )}

      {reRequest.isError && (
        <p className="text-sm text-danger">
          {reRequest.error instanceof ApiError ? reRequest.error.message : "재요청 처리에 실패했습니다."}
        </p>
      )}

      {!isAuthor && (
        <div className="space-y-2">
          <Textarea
            value={message}
            onChange={(event) => setMessage(event.target.value)}
            rows={2}
            placeholder="이 질문에 추가로 필요한 정보를 요청하세요"
          />
          {createRequest.isError && (
            <p className="text-sm text-danger">
              {createRequest.error instanceof ApiError ? createRequest.error.message : "정보 요청을 보내지 못했습니다."}
            </p>
          )}
          <Button variant="secondary" onClick={handleCreate} disabled={createRequest.isPending || !message.trim()}>
            {createRequest.isPending ? "요청 중..." : "정보 요청"}
          </Button>
        </div>
      )}
    </section>
  );
}
