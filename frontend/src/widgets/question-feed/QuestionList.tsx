import { QuestionCard } from "./QuestionCard";
import type { QuestionSummary } from "@/features/question/api/question.types";

export function QuestionList({
  questions,
  emptyMessage,
  compact = false,
}: {
  questions: QuestionSummary[];
  emptyMessage: string;
  /** Side-panel density — see QuestionCard. */
  compact?: boolean;
}) {
  if (questions.length === 0) {
    return <p className="text-sm text-text-secondary">{emptyMessage}</p>;
  }
  return (
    <ul className="divide-y divide-border overflow-hidden rounded-xl border border-border bg-surface">
      {questions.map((question) => (
        <QuestionCard key={question.id} question={question} compact={compact} />
      ))}
    </ul>
  );
}
