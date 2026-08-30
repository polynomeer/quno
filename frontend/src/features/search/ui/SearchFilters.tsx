import { StatusBadge, type QuestionStatus } from "@/shared/ui/StatusBadge";
import { cn } from "@/shared/lib/cn";

const ALL_STATUSES: QuestionStatus[] = ["OPEN", "NEEDS_INFO", "UPDATED", "RESOLVED", "OUTDATED"];

/** Client-side Tags/Status filters over an already-fetched search result set — see ADR-0022
 * for why Answered/Date/Score filters and non-Relevance sort aren't here. */
export function SearchFilters({
  availableTags,
  selectedTags,
  onToggleTag,
  selectedStatuses,
  onToggleStatus,
}: {
  availableTags: string[];
  selectedTags: string[];
  onToggleTag: (tag: string) => void;
  selectedStatuses: QuestionStatus[];
  onToggleStatus: (status: QuestionStatus) => void;
}) {
  return (
    <div className="space-y-2">
      <div className="flex flex-wrap items-center gap-2">
        <span className="text-xs font-medium text-text-secondary">Status</span>
        {ALL_STATUSES.map((status) => (
          <button
            key={status}
            type="button"
            onClick={() => onToggleStatus(status)}
            className={cn(!selectedStatuses.includes(status) && "opacity-40 hover:opacity-70")}
          >
            <StatusBadge status={status} className={selectedStatuses.includes(status) ? "ring-2 ring-brand" : ""} />
          </button>
        ))}
      </div>
      {availableTags.length > 0 && (
        <div className="flex flex-wrap items-center gap-2">
          <span className="text-xs font-medium text-text-secondary">Tags</span>
          {availableTags.map((tag) => (
            <button
              key={tag}
              type="button"
              onClick={() => onToggleTag(tag)}
              className={cn(
                "rounded-md px-2 py-0.5 text-xs font-medium",
                selectedTags.includes(tag)
                  ? "bg-brand text-brand-foreground"
                  : "bg-surface-subtle text-text-secondary hover:bg-border/40",
              )}
            >
              {tag}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
