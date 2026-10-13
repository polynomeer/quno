import { BadgeChip } from "./BadgeChip";
import type { Badge } from "../api/badge.types";
import { EmptyState } from "@/shared/ui/EmptyState";

export function BadgeList({ badges, emptyMessage }: { badges: Badge[]; emptyMessage: string }) {
  if (badges.length === 0) {
    return <EmptyState message={emptyMessage} inline />;
  }

  return (
    <div className="flex flex-wrap gap-1.5">
      {badges.map((badge) => (
        <BadgeChip key={badge.type} badge={badge} />
      ))}
    </div>
  );
}
