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
  // bg-brand/10(불투명도 트릭)은 text-brand와 4.48:1로 WCAG AA(4.5:1) 기준을 아슬아슬하게
  // 밑돌았다(axe-core로 발견, quality-improvement-plan.md Q-3) — 다른 세 톤처럼 전용 subtle
  // 배경 토큰(--brand-subtle)을 둬서 4.7:1 이상으로 여유를 확보했다.
  UPDATED: "bg-brand-subtle text-brand",
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
