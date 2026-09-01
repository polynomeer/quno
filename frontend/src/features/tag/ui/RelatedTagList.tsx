import { TagChip } from "@/shared/ui/TagChip";
import type { Tag } from "@/entities/tag/model/tag.types";

export function RelatedTagList({ tags }: { tags: Tag[] }) {
  if (tags.length === 0) {
    return <p className="text-sm text-text-secondary">관련 태그가 없습니다.</p>;
  }

  return (
    <div className="flex flex-wrap gap-1">
      {tags.map((tag) => (
        <TagChip key={tag.id} name={tag.name} />
      ))}
    </div>
  );
}
