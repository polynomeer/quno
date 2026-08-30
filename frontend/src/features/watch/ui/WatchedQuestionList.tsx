import Link from "next/link";
import { StatusBadge } from "@/shared/ui/StatusBadge";
import type { WatchedQuestion } from "../api/watch.types";

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
    return <p className="text-sm text-text-secondary">{emptyMessage}</p>;
  }

  return (
    <ul className="space-y-3">
      {questions.map((question) => (
        <li key={question.questionId} className="rounded-lg border border-border p-4">
          <Link href={`/questions/${question.questionId}`} className="font-medium hover:underline">
            {question.title}
          </Link>
          <div className="mt-2">
            <StatusBadge status={question.status} />
          </div>
        </li>
      ))}
    </ul>
  );
}
