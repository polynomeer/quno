"use client";

import { useState } from "react";
import { useCluster } from "../hooks/useCluster";
import { useMarkAsSameProblem } from "../hooks/useMarkAsSameProblem";
import { useDesignateSuperAnswer } from "../hooks/useDesignateSuperAnswer";
import { QuestionList } from "@/widgets/question-feed/QuestionList";
import { Input } from "@/shared/ui/Input";
import { Button } from "@/shared/ui/Button";
import { ApiError } from "@/shared/api/api-error";

/**
 * Cluster/Super Answer — like QPR, this has no design.md precedent (ADR-0016, api-design.md
 * §질문-cluster-super-answer). The backend only accepts a raw question id to join, not a search
 * picker, so this panel mirrors that rather than inventing a richer UX the API can't back.
 */
export function ClusterPanel({
  questionId,
  acceptedAnswerId,
}: {
  questionId: number;
  acceptedAnswerId: number | null;
}) {
  const { data: cluster } = useCluster(questionId);
  const markAsSameProblem = useMarkAsSameProblem(questionId);
  const designateSuperAnswer = useDesignateSuperAnswer(questionId, cluster?.clusterId ?? 0);
  const [relatedId, setRelatedId] = useState("");

  function handleJoin() {
    const id = Number(relatedId);
    if (!id) return;
    markAsSameProblem.mutate(id, { onSuccess: () => setRelatedId("") });
  }

  const otherMembers = cluster?.members.filter((m) => m.id !== questionId) ?? [];
  const canDesignateSuperAnswer =
    cluster && acceptedAnswerId !== null && cluster.representativeAnswerId !== acceptedAnswerId;

  return (
    <section className="space-y-3 rounded-lg border border-border p-4">
      <h2 className="text-sm font-semibold text-text-secondary">Cluster (같은 문제로 표시된 질문)</h2>

      {cluster && (
        <div className="space-y-2">
          {cluster.representativeAnswerId && (
            <p className="text-sm text-success">
              ✓ Super Answer 지정됨 (답변 #{cluster.representativeAnswerId})
            </p>
          )}
          <QuestionList questions={otherMembers} emptyMessage="같은 클러스터의 다른 질문이 없습니다." />
          {canDesignateSuperAnswer && (
            <Button
              variant="secondary"
              onClick={() => designateSuperAnswer.mutate(acceptedAnswerId)}
              disabled={designateSuperAnswer.isPending}
            >
              {designateSuperAnswer.isPending ? "지정 중..." : "채택된 답변을 Super Answer로 지정"}
            </Button>
          )}
          {designateSuperAnswer.isError && (
            <p className="text-sm text-danger">
              {designateSuperAnswer.error instanceof ApiError
                ? designateSuperAnswer.error.message
                : "Super Answer 지정에 실패했습니다."}
            </p>
          )}
        </div>
      )}

      <div className="flex gap-2">
        <Input
          value={relatedId}
          onChange={(event) => setRelatedId(event.target.value)}
          placeholder="같은 문제인 질문 ID"
          inputMode="numeric"
        />
        <Button variant="secondary" onClick={handleJoin} disabled={markAsSameProblem.isPending || !relatedId}>
          {markAsSameProblem.isPending ? "표시 중..." : "같은 문제로 표시"}
        </Button>
      </div>
      {markAsSameProblem.isError && (
        <p className="text-sm text-danger">
          {markAsSameProblem.error instanceof ApiError ? markAsSameProblem.error.message : "표시하지 못했습니다."}
        </p>
      )}
    </section>
  );
}
