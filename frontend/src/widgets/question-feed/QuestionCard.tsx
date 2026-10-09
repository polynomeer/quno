import Link from "next/link";
import { StatusBadge } from "@/shared/ui/StatusBadge";
import { TagChip } from "@/shared/ui/TagChip";
import { isWithin, relativeTime } from "@/shared/lib/relative-time";
import { cn } from "@/shared/lib/cn";
import type { QuestionSummary } from "@/features/question/api/question.types";

const DAY_MS = 24 * 60 * 60 * 1000;

/** "1.8k" style from 1,000 up — the design canvas's compact view count. */
function formatCount(count: number): string {
  if (count < 1000) return String(count);
  return `${(count / 1000).toFixed(count >= 10_000 ? 0 : 1).replace(/\.0$/, "")}k`;
}

/** Card info priority per design.md #9: title/status/tags first. Score (Phase 11) is shown as
 * plain text, not an interactive VoteControl — voting from a list card is out of scope.
 *
 * Layout follows the design canvas's Living Question Card (ADR-0061/0062): a numeric column
 * (score, answers) on the left; state, revision and "how recently it moved" above the title.
 * `compact` is for side panels, which are too narrow for the numeric column — the counts move
 * into the meta row instead. */
export function QuestionCard({ question, compact = false }: { question: QuestionSummary; compact?: boolean }) {
  // `updatedAt` moves on any change to the question (revision, accept, cluster, outdated), so it
  // reads as "last activity" rather than "last edit".
  const movedRecently = isWithin(question.updatedAt, DAY_MS);

  return (
    <li className={cn("flex gap-4 transition-colors hover:bg-canvas/60", compact ? "p-4" : "px-4 py-4 sm:gap-5 sm:px-5")}>
      {!compact && (
        <div className="flex w-16 shrink-0 flex-col items-end gap-1.5 pt-0.5 text-right text-xs text-text-secondary">
          <span>
            <span className="font-mono text-sm font-semibold text-text-primary">{question.score}</span> score
          </span>
          <span
            className={cn(
              "rounded px-1.5",
              question.hasAcceptedAnswer
                ? "bg-success-strong text-surface"
                : question.answerCount > 0
                  ? "border border-success-strong text-success"
                  : "text-warning",
            )}
          >
            <span className="font-mono text-sm font-semibold">{question.answerCount}</span> 답변
          </span>
          <span>
            <span className="font-mono">{formatCount(question.viewCount)}</span> 조회
          </span>
        </div>
      )}
      <div className="min-w-0 flex-1 space-y-2">
        <div className="flex flex-wrap items-center gap-2 text-xs">
          <StatusBadge status={question.status} />
          {compact ? (
            <span className="font-mono text-text-secondary">
              {question.score} score · {question.answerCount} 답변
            </span>
          ) : (
            <>
              {question.versionNumber > 1 && (
                <span className="inline-flex h-[22px] items-center rounded border border-border-strong px-2 font-mono text-[11px] text-text-body">
                  rev {question.versionNumber}
                </span>
              )}
              <span className="inline-flex items-center gap-1.5 text-text-secondary">
                <span
                  aria-hidden="true"
                  className={cn("size-1.5 rounded-full", movedRecently ? "bg-live" : "bg-border-strong")}
                />
                {relativeTime(question.updatedAt)} 활동
              </span>
            </>
          )}
        </div>
        <Link
          href={`/questions/${question.id}`}
          className={cn(
            "block leading-snug font-semibold text-text-primary hover:text-brand",
            compact ? "text-[15px]" : "text-[17px]",
          )}
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
