import { QuestionCard } from "./QuestionCard";
import type { QuestionSummary } from "@/features/question/api/question.types";

export function QuestionList({ questions, emptyMessage }: { questions: QuestionSummary[]; emptyMessage: string }) {
  if (questions.length === 0) {
    return <p className="text-sm text-text-secondary">{emptyMessage}</p>;
  }
  return (
    <ul className="space-y-3">
      {questions.map((question) => (
        <QuestionCard key={question.id} question={question} />
      ))}
    </ul>
  );
}
