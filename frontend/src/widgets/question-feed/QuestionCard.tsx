import Link from "next/link";
import { StatusBadge } from "@/shared/ui/StatusBadge";
import { TagChip } from "@/shared/ui/TagChip";
import type { QuestionSummary } from "@/features/question/api/question.types";

/** Card info priority per design.md #9: title/status/tags first, no score/answer/view count —
 * the backend's QuestionSummary shape doesn't carry those (see ADR-0020 gap analysis). */
export function QuestionCard({ question }: { question: QuestionSummary }) {
  return (
    <li className="rounded-lg border border-border p-4 transition-colors hover:border-text-secondary/40">
      <Link href={`/questions/${question.id}`} className="font-medium hover:underline">
        {question.title}
      </Link>
      <div className="mt-2 flex flex-wrap items-center gap-2">
        <StatusBadge status={question.status} />
        {question.tags.map((tag) => (
          <TagChip key={tag} name={tag} />
        ))}
      </div>
    </li>
  );
}
