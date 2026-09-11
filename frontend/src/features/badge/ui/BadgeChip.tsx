import { cn } from "@/shared/lib/cn";
import { describeBadge } from "../lib/describe-badge";
import type { Badge, BadgeTier } from "../api/badge.types";

const tierClasses: Record<BadgeTier, string> = {
  BRONZE: "bg-warning-subtle text-warning",
  SILVER: "bg-surface-subtle text-text-secondary",
  // bg-brand/10은 text-brand와 WCAG AA 기준(4.5:1)에 못 미쳐(StatusBadge와 같은 문제,
  // quality-improvement-plan.md Q-3) 전용 subtle 배경 토큰으로 바꿨다.
  GOLD: "bg-brand-subtle text-brand",
};

export function BadgeChip({ badge }: { badge: Badge }) {
  const { name, description } = describeBadge(badge.type);

  return (
    <span
      title={description}
      className={cn("inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium", tierClasses[badge.tier])}
    >
      {name}
    </span>
  );
}
