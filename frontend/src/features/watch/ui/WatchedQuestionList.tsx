import Link from "next/link";
import { StatusBadge } from "@/shared/ui/StatusBadge";
import type { WatchedQuestion } from "../api/watch.types";
import { EmptyState } from "@/shared/ui/EmptyState";

/** WatchedQuestionResponse has no tags, unlike QuestionSummary — a dedicated list instead of
 * reusing widgets/question-feed/QuestionCard, which requires a `tags` field. */
export function WatchedQuestionList({
  questions,
  emptyMessage,
}: {
  questions: WatchedQuestion[];
  emptyMessage: string;
}) {
  if (questions.length === 0) {
    return <EmptyState message={emptyMessage} />;
  }

  return (
    // Same shell as QuestionList (one card, divided rows) and the same state-above-title order as
    // QuestionCard — minus its numeric column, since this response carries no score or counts.
    <ul className="divide-y divide-border overflow-hidden rounded-[20px] border border-border bg-surface">
      {questions.map((question) => (
        <li key={question.questionId} className="space-y-2 px-4 py-4 transition-colors hover:bg-canvas/60 sm:px-5">
          <StatusBadge status={question.status} />
          <Link
            href={`/questions/${question.questionId}`}
            className="block text-[17px] leading-snug font-semibold text-text-primary hover:text-brand"
          >
            {question.title}
          </Link>
        </li>
      ))}
    </ul>
  );
}
