"use client";

import { use } from "react";
import { useRequireAuth } from "@/features/auth/hooks/useRequireAuth";
import { useQuestion } from "@/features/question/hooks/useQuestion";
import { useRelatedQuestions } from "@/features/question/hooks/useRelatedQuestions";
import { useAnswers } from "@/features/answer/hooks/useAnswers";
import { QuestionMeta } from "@/features/question/ui/QuestionMeta";
import { AnswerCard } from "@/features/answer/ui/AnswerCard";
import { QuestionList } from "@/widgets/question-feed/QuestionList";
import { StatusBadge } from "@/shared/ui/StatusBadge";
import { TagChip } from "@/shared/ui/TagChip";
import { MarkdownContent } from "@/shared/ui/MarkdownContent";
import { Skeleton } from "@/shared/ui/Skeleton";

export default function QuestionDetailPage({ params }: PageProps<"/questions/[id]">) {
  const { id } = use(params);
  const questionId = Number(id);
  const { isLoading: authLoading } = useRequireAuth();
  const { data: question, isLoading, isError } = useQuestion(questionId);
  const { data: related } = useRelatedQuestions(questionId);
  const { data: answers } = useAnswers(questionId);

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
    if (a.isAccepted !== b.isAccepted) return a.isAccepted ? -1 : 1;
    return new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime();
  });

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
          <h2 className="text-lg font-semibold">{sortedAnswers.length} Answers</h2>
          {sortedAnswers.length === 0 ? (
            <p className="text-sm text-text-secondary">아직 답변이 없습니다.</p>
          ) : (
            <ul className="space-y-3">
              {sortedAnswers.map((answer) => (
                <AnswerCard key={answer.id} answer={answer} />
              ))}
            </ul>
          )}
        </section>
      </div>

      <aside className="space-y-3">
        <h2 className="text-sm font-semibold text-text-secondary">Related Questions</h2>
        <QuestionList questions={related ?? []} emptyMessage="관련 질문이 없습니다." />
      </aside>
    </div>
  );
}
