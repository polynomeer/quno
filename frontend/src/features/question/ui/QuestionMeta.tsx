import Link from "next/link";
import { relativeTime } from "@/shared/lib/relative-time";

export function QuestionMeta({
  questionId,
  createdAt,
  updatedAt,
  versionNumber,
}: {
  questionId: number;
  createdAt: string;
  updatedAt: string;
  versionNumber: number;
}) {
  const edited = versionNumber > 1;
  return (
    <p className="flex flex-wrap items-center gap-x-3 gap-y-1 text-[13px] text-text-secondary">
      <span>asked {relativeTime(createdAt)}</span>
      {edited && (
        // "Knowledge moved recently" outranks "when it was created" (design.md #3.2), so the
        // revision gets the brand chip from the design canvas instead of a plain underline.
        <Link
          href={`/questions/${questionId}/versions`}
          className="inline-flex h-6 items-center rounded bg-brand-subtle px-2 font-mono text-xs font-semibold text-brand-on-subtle hover:underline"
        >
          edited {relativeTime(updatedAt)} · revision {versionNumber}
        </Link>
      )}
    </p>
  );
}
