import Link from "next/link";
import { StatusBadge } from "@/shared/ui/StatusBadge";
import { TagChip } from "@/shared/ui/TagChip";
import type { QuestionSummary } from "@/features/question/api/question.types";

/** Card info priority per design.md #9: title/status/tags first. Score (Phase 11) is shown as
 * plain text, not an interactive VoteControl — voting from a list card is out of scope.
 *
 * Layout follows the design canvas's Living Question Card (ADR-0061): a quiet numeric column on
 * the left, state + title + tags on the right. The canvas also shows answer/view counts, revision
 * and activity lines — `QuestionSummary` doesn't carry those yet, so they're left out rather than
 * faked. */
export function QuestionCard({ question }: { question: QuestionSummary }) {
  return (
    <li className="flex gap-4 px-4 py-4 transition-colors hover:bg-canvas/60 sm:gap-5 sm:px-5">
      <div className="flex w-12 shrink-0 flex-col items-end pt-0.5 text-right">
        <span className="font-mono text-base font-semibold text-text-primary">{question.score}</span>
        <span className="text-xs text-text-secondary">score</span>
      </div>
      <div className="min-w-0 flex-1 space-y-2">
        <StatusBadge status={question.status} />
        <Link
          href={`/questions/${question.id}`}
          className="block text-[17px] leading-snug font-semibold text-text-primary hover:text-brand"
        >
          {question.title}
        </Link>
        {question.tags.length > 0 && (
          <div className="flex flex-wrap gap-1.5">
            {question.tags.map((tag) => (
              <TagChip key={tag} name={tag} />
            ))}
          </div>
        )}
      </div>
    </li>
  );
}
