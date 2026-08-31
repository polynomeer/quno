"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useQuestionGraph } from "../hooks/useQuestionGraph";
import { useForkQuestion } from "../hooks/useForkQuestion";
import { Button } from "@/shared/ui/Button";
import { ApiError } from "@/shared/api/api-error";

/**
 * Fork/lineage view backed by `GET /questions/{id}/graph` (Phase 18, ADR-0030). Only reads the
 * Fork lineage fields from that response — `clusterMembers`/`relatedQuestions` are deliberately
 * left unused here since Cluster (`ClusterPanel`) and Related Questions (the sidebar) already
 * have their own dedicated UI on this page; showing them again here would just duplicate that.
 */
export function ForkPanel({ questionId }: { questionId: number }) {
  const { data: graph } = useQuestionGraph(questionId);
  const forkQuestion = useForkQuestion(questionId);
  const router = useRouter();

  async function handleFork() {
    const result = await forkQuestion.mutateAsync();
    router.push(`/questions/${result.id}`);
  }

  return (
    <section className="space-y-3 rounded-lg border border-border p-4">
      <div className="flex items-center justify-between">
        <h2 className="text-sm font-semibold text-text-secondary">Fork</h2>
        <Button
          variant="secondary"
          className="px-2 py-1 text-xs"
          onClick={handleFork}
          disabled={forkQuestion.isPending}
        >
          {forkQuestion.isPending ? "포크 중..." : "Fork this question"}
        </Button>
      </div>

      {forkQuestion.isError && (
        <p className="text-sm text-danger">
          {forkQuestion.error instanceof ApiError ? forkQuestion.error.message : "포크하지 못했습니다."}
        </p>
      )}

      {graph?.forkedFrom && (
        <p className="text-sm text-text-secondary">
          Forked from{" "}
          <Link href={`/questions/${graph.forkedFrom.id}`} className="underline hover:text-text-primary">
            {graph.forkedFrom.title}
          </Link>
        </p>
      )}

      {graph && graph.forks.length > 0 && (
        <div className="space-y-1">
          <p className="text-xs font-medium text-text-secondary">Forks ({graph.forks.length})</p>
          <ul className="space-y-1">
            {graph.forks.map((fork) => (
              <li key={fork.id}>
                <Link href={`/questions/${fork.id}`} className="text-sm underline hover:text-text-primary">
                  {fork.title}
                </Link>
              </li>
            ))}
          </ul>
        </div>
      )}
    </section>
  );
}
