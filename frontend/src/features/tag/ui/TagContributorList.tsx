import Link from "next/link";
import type { TagContributor } from "@/entities/tag/model/tag.types";

export function TagContributorList({ contributors }: { contributors: TagContributor[] }) {
  if (contributors.length === 0) {
    return <p className="text-sm text-text-secondary">아직 답변자가 없습니다.</p>;
  }

  return (
    <ol className="space-y-1">
      {contributors.map((contributor, index) => (
        <li key={contributor.userId} className="flex items-center gap-2 text-sm">
          <span className="w-4 text-text-secondary">{index + 1}</span>
          <Link href={`/users/${contributor.userId}`} className="font-medium hover:underline">
            {contributor.nickname}
          </Link>
          <span className="text-text-secondary">답변 {contributor.answerCount}개</span>
        </li>
      ))}
    </ol>
  );
}
