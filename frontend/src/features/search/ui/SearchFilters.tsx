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
    <div className="space-y-3 rounded-[20px] border border-border bg-surface p-5">
      <div className="flex flex-wrap items-center gap-2">
        <span className="w-12 text-xs font-semibold text-text-primary">Status</span>
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
          <span className="w-12 text-xs font-semibold text-text-primary">Tags</span>
          {availableTags.map((tag) => (
            <button
              key={tag}
              type="button"
              onClick={() => onToggleTag(tag)}
              className={cn(
                "inline-flex h-[22px] items-center rounded px-2 font-mono text-xs font-semibold transition-colors",
                selectedTags.includes(tag)
                  ? "bg-brand text-brand-foreground"
                  : "bg-surface-subtle text-text-body hover:text-brand",
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
