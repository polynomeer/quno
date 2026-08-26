import { TagChip } from "@/shared/ui/TagChip";
import type { TagSpike, TagTrend } from "@/features/dashboard/api/dashboard.types";

export function TrendingTagsPanel({ tags, spikes }: { tags: TagTrend[]; spikes: TagSpike[] }) {
  if (tags.length === 0 && spikes.length === 0) {
    return null;
  }
  return (
    <div className="space-y-4 rounded-lg border border-border p-4">
      {tags.length > 0 && (
        <div>
          <h2 className="mb-2 text-sm font-semibold text-text-secondary">Trending Tags</h2>
          <ul className="space-y-2">
            {tags.map((tag) => (
              <li key={tag.id} className="flex items-center justify-between text-sm">
                <TagChip name={tag.name} />
                <span className="text-text-secondary">{tag.questionCount}개 질문</span>
              </li>
            ))}
          </ul>
        </div>
      )}
      {spikes.length > 0 && (
        <div>
          <h2 className="mb-2 text-sm font-semibold text-text-secondary">Trending Errors</h2>
          <ul className="space-y-2">
            {spikes.map((spike) => (
              <li key={spike.id} className="flex items-center justify-between text-sm">
                <TagChip name={spike.name} />
                <span className="text-warning">{spike.spikeRatio.toFixed(1)}x 급증</span>
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}
