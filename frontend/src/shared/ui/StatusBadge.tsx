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

const toneClasses: Record<QuestionStatus, string> = {
  OPEN: "bg-surface-subtle text-text-secondary",
  NEEDS_INFO: "bg-warning-subtle text-warning",
  UPDATED: "bg-brand/10 text-brand",
  RESOLVED: "bg-success-subtle text-success",
  OUTDATED: "bg-danger-subtle text-danger",
};

export function StatusBadge({ status, className }: { status: QuestionStatus; className?: string }) {
  return (
    <span
      className={cn(
        "inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium",
        toneClasses[status],
        className,
      )}
    >
      {labels[status]}
    </span>
  );
}
