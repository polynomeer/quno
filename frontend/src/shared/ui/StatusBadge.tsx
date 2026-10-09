import { cn } from "@/shared/lib/cn";

/**
 * Matches the backend's QuestionStatus (docs/architecture/domain-model.md) — not the richer
 * NEW/ACTIVE/UNANSWERED/SOLVED/DUPLICATE model in docs/frontend/design.md #31, which the
 * backend doesn't implement yet (see design.md's per-status "현재 백엔드 대응" column).
 */
export type QuestionStatus = "OPEN" | "NEEDS_INFO" | "UPDATED" | "RESOLVED" | "OUTDATED";

const labels: Record<QuestionStatus, string> = {
  OPEN: "Open",
  NEEDS_INFO: "Needs info",
  UPDATED: "Updated",
  RESOLVED: "Solved",
  OUTDATED: "Outdated",
};

// Tones follow the design canvas's state table (ADR-0061): color appears only to say what state a
// question is in. Each pair keeps its own subtle background so contrast stays ≥4.5:1 in both
// themes — the same reason UPDATED got --brand-subtle in ADR-0051.
const toneClasses: Record<QuestionStatus, string> = {
  OPEN: "border border-text-primary bg-surface text-text-primary",
  NEEDS_INFO: "bg-danger-subtle text-danger",
  UPDATED: "bg-brand-subtle text-brand-on-subtle",
  RESOLVED: "bg-success-subtle text-success",
  OUTDATED: "bg-surface-subtle text-text-body",
};

export function StatusBadge({ status, className }: { status: QuestionStatus; className?: string }) {
  return (
    <span
      className={cn(
        "inline-flex h-[22px] shrink-0 items-center gap-1 rounded px-2 text-xs font-semibold",
        toneClasses[status],
        className,
      )}
    >
      {status === "RESOLVED" && (
        <svg aria-hidden="true" viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="currentColor" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round">
          <path d="m5 12 5 5L20 7" />
        </svg>
      )}
      {labels[status]}
    </span>
  );
}
