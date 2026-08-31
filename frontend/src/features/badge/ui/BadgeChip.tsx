import { cn } from "@/shared/lib/cn";
import { describeBadge } from "../lib/describe-badge";
import type { Badge, BadgeTier } from "../api/badge.types";

const tierClasses: Record<BadgeTier, string> = {
  BRONZE: "bg-warning-subtle text-warning",
  SILVER: "bg-surface-subtle text-text-secondary",
  GOLD: "bg-brand/10 text-brand",
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
