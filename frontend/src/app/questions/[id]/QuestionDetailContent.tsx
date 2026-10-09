"use client";

import { useState } from "react";
import Link from "next/link";
import { ApiError, RequestTimeoutError } from "@/shared/api/api-error";
import { useSession } from "@/features/auth/hooks/useSession";
import { useQuestion } from "@/features/question/hooks/useQuestion";
import { useRelatedQuestions } from "@/features/question/hooks/useRelatedQuestions";
import { useRecordQuestionView } from "@/features/question/hooks/useRecordQuestionView";
import { useAnswers } from "@/features/answer/hooks/useAnswers";
import { useAcceptAnswer } from "@/features/answer/hooks/useAcceptAnswer";
import { QuestionMeta } from "@/features/question/ui/QuestionMeta";
import { OutdatedAction } from "@/features/question/ui/OutdatedAction";
import { AnswerCard } from "@/features/answer/ui/AnswerCard";
import { AnswerComposer } from "@/features/answer/ui/AnswerComposer";
import { WatchButton } from "@/features/watch/ui/WatchButton";
import { SaveButton } from "@/features/save/ui/SaveButton";
import { VoteControl } from "@/features/vote/ui/VoteControl";
import { CommentSection } from "@/features/comment/ui/CommentSection";
import { ReportButton } from "@/features/report/ui/ReportButton";
import { ReviewRequestPanel } from "@/features/review/ui/ReviewRequestPanel";
import { ClusterPanel } from "@/features/cluster/ui/ClusterPanel";
import { ForkPanel } from "@/features/question/ui/ForkPanel";
import { QuestionTimeline } from "@/features/question/ui/QuestionTimeline";
import { LiveChatPanel } from "@/features/live-chat/ui/LiveChatPanel";
import { QuestionList } from "@/widgets/question-feed/QuestionList";
import { StatusBadge } from "@/shared/ui/StatusBadge";
import { TagChip } from "@/shared/ui/TagChip";
import { MarkdownContent } from "@/shared/ui/MarkdownContent";
import { Skeleton } from "@/shared/ui/Skeleton";
import { FormError } from "@/shared/ui/FormError";

type AnswerSort = "best" | "newest" | "oldest" | "score";

/** All the interactivity for Question Detail — split out of `page.tsx` so that file can stay a
 * Server Component and export `generateMetadata` (Phase 31, ADR-0043). Nothing about the
 * behavior here changed, only where it lives. */
export function QuestionDetailContent({ questionId }: { questionId: number }) {
  const { data: me, isLoading: authLoading } = useSession();
  const { data: question, isLoading, isError, error } = useQuestion(questionId);
  const { data: related } = useRelatedQuestions(questionId);
  const { data: answers } = useAnswers(questionId);
  const acceptAnswer = useAcceptAnswer(questionId);
  const [sort, setSort] = useState<AnswerSort>("best");
  useRecordQuestionView(questionId);

  if (authLoading || isLoading) {
    return (
      <div className="space-y-4">
        <Skeleton className="h-8 w-2/3" />
        <Skeleton className="h-40 w-full" />
      </div>
    );
  }

  if (isError || !question) {
    const message =
      error instanceof ApiError && error.status === 404
        ? "질문을 찾을 수 없습니다."
        : error instanceof RequestTimeoutError
          ? "요청 시간이 초과됐습니다. 네트워크 상태를 확인하고 다시 시도해주세요."
          : "질문을 불러오지 못했습니다. 잠시 후 다시 시도해주세요.";
    return <p className="text-sm text-danger">{message}</p>;
  }

  const sortedAnswers = [...(answers ?? [])].sort((a, b) => {
    if (sort === "best" && a.isAccepted !== b.isAccepted) return a.isAccepted ? -1 : 1;
    if (sort === "score" && a.score !== b.score) return b.score - a.score;
    const byTime = new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime();
    return sort === "newest" ? -byTime : byTime;
  });

  const canAccept = Boolean(me && me.id === question.authorId);
  const acceptedAnswerId = answers?.find((a) => a.isAccepted)?.id ?? null;

  return (
    <div className="space-y-7">
      {/* Header per the design canvas (ADR-0061): state first, then the H1, then meta + tags. */}
      <header className="space-y-3 border-b border-border pb-6">
        <div className="flex flex-wrap items-center gap-2">
          <StatusBadge status={question.status} />
        </div>
        <h1 className="max-w-[900px] text-[26px] leading-snug font-bold tracking-tight sm:text-3xl">
          {question.title}
        </h1>
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div className="flex flex-wrap items-center gap-3">
            <QuestionMeta
              questionId={question.id}
              createdAt={question.createdAt}
              updatedAt={question.updatedAt}
              versionNumber={question.versionNumber}
              viewCount={question.viewCount}
            />
            {me && me.id !== question.authorId && <ReportButton targetType="QUESTION" targetId={question.id} />}
          </div>
          <div className="flex flex-wrap gap-1.5">
            {question.tags.map((tag) => (
              <TagChip key={tag} name={tag} />
            ))}
          </div>
        </div>
        {me && (
          <div className="flex flex-wrap items-center justify-between gap-2">
            <div className="flex items-center gap-2">
              <WatchButton questionId={question.id} />
              <SaveButton questionId={question.id} />
            </div>
            <OutdatedAction questionId={question.id} status={question.status} />
          </div>
        )}
      </header>

      <div className="grid grid-cols-1 gap-10 lg:grid-cols-[minmax(0,1fr)_300px]">
        <div className="min-w-0 space-y-10">
          <article className="flex gap-4 sm:gap-5">
            <div className="shrink-0">
              <VoteControl
                targetType="QUESTION"
                targetId={question.id}
                questionId={question.id}
                score={question.score}
                authorId={question.authorId}
              />
            </div>
            <div className="min-w-0 flex-1 space-y-4">
              <MarkdownContent>{question.body}</MarkdownContent>

              {(question.environment || question.logs) && (
                <details className="rounded-lg border border-border bg-surface p-3 text-sm">
                  <summary className="cursor-pointer font-medium text-text-body">환경 / 로그</summary>
                  {question.environment && (
                    <div className="mt-2">
                      <p className="text-xs font-medium text-text-secondary">Environment</p>
                      <pre className="mt-1 overflow-x-auto rounded-md bg-code-bg p-3 font-mono text-xs text-code-fg">
                        {question.environment}
                      </pre>
                    </div>
                  )}
                  {question.logs && (
                    <div className="mt-2">
                      <p className="text-xs font-medium text-text-secondary">Logs</p>
                      <pre className="mt-1 overflow-x-auto rounded-md bg-code-bg p-3 font-mono text-xs text-code-fg">
                        {question.logs}
                      </pre>
                    </div>
                  )}
                </details>
              )}

              <CommentSection targetType="QUESTION" targetId={question.id} />
            </div>
          </article>

          {me && (
            <ReviewRequestPanel
              questionId={question.id}
              questionAuthorId={question.authorId}
              questionVersionNumber={question.versionNumber}
              currentUserId={me.id}
            />
          )}

          <section className="space-y-4">
            <div className="flex flex-wrap items-end justify-between gap-3 border-b border-border pb-3">
              <h2 className="text-xl font-bold">{sortedAnswers.length} Answers</h2>
              {sortedAnswers.length > 1 && (
                <label className="flex items-center gap-1 text-sm text-text-secondary">
                  sort:
                  <select
                    className="h-9 rounded-lg border border-border-strong bg-surface px-2 text-text-primary"
                    value={sort}
                    onChange={(event) => setSort(event.target.value as AnswerSort)}
                  >
                    <option value="best">Best</option>
                    <option value="newest">Newest</option>
                    <option value="oldest">Oldest</option>
                    <option value="score">Score</option>
                  </select>
                </label>
              )}
            </div>

            <FormError error={acceptAnswer.error} fallback="답변을 채택하지 못했습니다." />

            {sortedAnswers.length === 0 ? (
              <p className="text-sm text-text-secondary">아직 답변이 없습니다.</p>
            ) : (
              <ul className="space-y-4">
                {sortedAnswers.map((answer) => (
                  <AnswerCard
                    key={answer.id}
                    answer={answer}
                    canAccept={canAccept}
                    isAccepting={acceptAnswer.isPending && acceptAnswer.variables === answer.id}
                    onAccept={() => acceptAnswer.mutate(answer.id)}
                    showEngagement
                  />
                ))}
              </ul>
            )}
          </section>

          {me ? (
            <AnswerComposer questionId={questionId} />
          ) : (
            <p className="text-sm text-text-secondary">
              <Link
                href={`/login?redirectTo=/questions/${questionId}`}
                className="text-brand underline hover:no-underline"
              >
                로그인
              </Link>
              하고 답변을 작성하세요.
            </p>
          )}

          {me && (
            <>
              <ClusterPanel questionId={question.id} acceptedAnswerId={acceptedAnswerId} />
              <ForkPanel questionId={question.id} />
              <LiveChatPanel questionId={question.id} />
            </>
          )}
        </div>

        <aside className="space-y-6">
          <QuestionTimeline questionId={question.id} />
          <div className="space-y-3">
            <h2 className="text-sm font-semibold text-text-primary">Related Questions</h2>
            <QuestionList questions={related ?? []} emptyMessage="관련 질문이 없습니다." compact />
          </div>
        </aside>
      </div>
    </div>
  );
}
