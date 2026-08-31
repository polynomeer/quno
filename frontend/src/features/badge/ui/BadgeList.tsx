import { BadgeChip } from "./BadgeChip";
import type { Badge } from "../api/badge.types";

export function BadgeList({ badges, emptyMessage }: { badges: Badge[]; emptyMessage: string }) {
  if (badges.length === 0) {
    return <p className="text-sm text-text-secondary">{emptyMessage}</p>;
  }

  return (
    <div className="flex flex-wrap gap-1.5">
      {badges.map((badge) => (
        <BadgeChip key={badge.type} badge={badge} />
      ))}
    </div>
  );
}
