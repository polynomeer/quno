import { QuestionCard } from "./QuestionCard";
import type { QuestionSummary } from "@/features/question/api/question.types";
import { EmptyState } from "@/shared/ui/EmptyState";

export function QuestionList({
  questions,
  emptyMessage,
  compact = false,
  emptyInline = false,
}: {
  questions: QuestionSummary[];
  emptyMessage: string;
  /** Side-panel density — see QuestionCard. */
  compact?: boolean;
  /** Plain-line empty state for lists that already sit inside a card (compact implies it). */
  emptyInline?: boolean;
}) {
  if (questions.length === 0) {
    return <EmptyState message={emptyMessage} inline={compact || emptyInline} />;
  }
  return (
    // Separate cards rather than a divided table — web design v2 (ADR-0071).
    <ul className="space-y-3">
      {questions.map((question) => (
        <QuestionCard key={question.id} question={question} compact={compact} />
      ))}
    </ul>
  );
}
