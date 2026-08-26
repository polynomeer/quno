"use client";

import { use, useState } from "react";
import { useRequireAuth } from "@/features/auth/hooks/useRequireAuth";
import { useQuestion } from "@/features/question/hooks/useQuestion";
import { useRelatedQuestions } from "@/features/question/hooks/useRelatedQuestions";
import { useAnswers } from "@/features/answer/hooks/useAnswers";
import { useAcceptAnswer } from "@/features/answer/hooks/useAcceptAnswer";
import { QuestionMeta } from "@/features/question/ui/QuestionMeta";
import { AnswerCard } from "@/features/answer/ui/AnswerCard";
import { AnswerComposer } from "@/features/answer/ui/AnswerComposer";
import { QuestionList } from "@/widgets/question-feed/QuestionList";
import { StatusBadge } from "@/shared/ui/StatusBadge";
import { TagChip } from "@/shared/ui/TagChip";
import { MarkdownContent } from "@/shared/ui/MarkdownContent";
import { Skeleton } from "@/shared/ui/Skeleton";
import { ApiError } from "@/shared/api/api-error";

type AnswerSort = "best" | "newest" | "oldest";

export default function QuestionDetailPage({ params }: PageProps<"/questions/[id]">) {
  const { id } = use(params);
  const questionId = Number(id);
  const { me, isLoading: authLoading } = useRequireAuth();
  const { data: question, isLoading, isError } = useQuestion(questionId);
  const { data: related } = useRelatedQuestions(questionId);
  const { data: answers } = useAnswers(questionId);
  const acceptAnswer = useAcceptAnswer(questionId);
  const [sort, setSort] = useState<AnswerSort>("best");

  if (authLoading || isLoading) {
    return (
      <div className="space-y-4">
        <Skeleton className="h-8 w-2/3" />
        <Skeleton className="h-40 w-full" />
      </div>
    );
  }

  if (isError || !question) {
    return <p className="text-sm text-danger">질문을 찾을 수 없습니다.</p>;
  }

  const sortedAnswers = [...(answers ?? [])].sort((a, b) => {
    if (sort === "best" && a.isAccepted !== b.isAccepted) return a.isAccepted ? -1 : 1;
    const byTime = new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime();
    return sort === "newest" ? -byTime : byTime;
  });

  const canAccept = Boolean(me && me.id === question.authorId);

  return (
    <div className="grid grid-cols-1 gap-6 lg:grid-cols-[1fr_280px]">
      <div className="space-y-6">
        <header className="space-y-2">
          <div className="flex flex-wrap items-center gap-2">
            <StatusBadge status={question.status} />
            <h1 className="text-2xl font-semibold">{question.title}</h1>
          </div>
          <div className="flex flex-wrap gap-1">
            {question.tags.map((tag) => (
              <TagChip key={tag} name={tag} />
            ))}
          </div>
          <QuestionMeta
            questionId={question.id}
            createdAt={question.createdAt}
            updatedAt={question.updatedAt}
            versionNumber={question.versionNumber}
          />
        </header>

        <MarkdownContent>{question.body}</MarkdownContent>

        {(question.environment || question.logs) && (
          <details className="rounded-md border border-border p-3 text-sm">
            <summary className="cursor-pointer font-medium text-text-secondary">환경 / 로그</summary>
            {question.environment && (
              <div className="mt-2">
                <p className="text-xs font-medium text-text-secondary">Environment</p>
                <pre className="mt-1 overflow-x-auto rounded bg-surface-subtle p-2 text-xs">{question.environment}</pre>
              </div>
            )}
            {question.logs && (
              <div className="mt-2">
                <p className="text-xs font-medium text-text-secondary">Logs</p>
                <pre className="mt-1 overflow-x-auto rounded bg-surface-subtle p-2 text-xs">{question.logs}</pre>
              </div>
            )}
          </details>
        )}

        <section className="space-y-3">
          <div className="flex items-center justify-between">
            <h2 className="text-lg font-semibold">{sortedAnswers.length} Answers</h2>
            {sortedAnswers.length > 1 && (
              <label className="flex items-center gap-1 text-sm text-text-secondary">
                sort:
                <select
                  className="rounded-md border border-border bg-surface px-2 py-1"
                  value={sort}
                  onChange={(event) => setSort(event.target.value as AnswerSort)}
                >
                  <option value="best">Best</option>
                  <option value="newest">Newest</option>
                  <option value="oldest">Oldest</option>
                </select>
              </label>
            )}
          </div>

          {acceptAnswer.isError && (
            <p className="text-sm text-danger">
              {acceptAnswer.error instanceof ApiError ? acceptAnswer.error.message : "답변을 채택하지 못했습니다."}
            </p>
          )}

          {sortedAnswers.length === 0 ? (
            <p className="text-sm text-text-secondary">아직 답변이 없습니다.</p>
          ) : (
            <ul className="space-y-3">
              {sortedAnswers.map((answer) => (
                <AnswerCard
                  key={answer.id}
                  answer={answer}
                  canAccept={canAccept}
                  isAccepting={acceptAnswer.isPending && acceptAnswer.variables === answer.id}
                  onAccept={() => acceptAnswer.mutate(answer.id)}
                />
              ))}
            </ul>
          )}
        </section>

        <AnswerComposer questionId={questionId} />
      </div>

      <aside className="space-y-3">
        <h2 className="text-sm font-semibold text-text-secondary">Related Questions</h2>
        <QuestionList questions={related ?? []} emptyMessage="관련 질문이 없습니다." />
      </aside>
    </div>
  );
}
