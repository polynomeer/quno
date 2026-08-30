import Link from "next/link";
import { StatusBadge } from "@/shared/ui/StatusBadge";
import { TagChip } from "@/shared/ui/TagChip";
import type { QuestionSummary } from "@/features/question/api/question.types";

/** Card info priority per design.md #9: title/status/tags first. Score (Phase 11) is shown as
 * plain text, not an interactive VoteControl — voting from a list card is out of scope. */
export function QuestionCard({ question }: { question: QuestionSummary }) {
  return (
    <li className="rounded-lg border border-border p-4 transition-colors hover:border-text-secondary/40">
      <div className="flex items-start justify-between gap-2">
        <Link href={`/questions/${question.id}`} className="font-medium hover:underline">
          {question.title}
        </Link>
        <span className="shrink-0 text-xs text-text-secondary">score {question.score}</span>
      </div>
      <div className="mt-2 flex flex-wrap items-center gap-2">
        <StatusBadge status={question.status} />
        {question.tags.map((tag) => (
          <TagChip key={tag} name={tag} />
        ))}
      </div>
    </li>
  );
}
