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
    <p className="text-sm text-text-secondary">
      asked {relativeTime(createdAt)}
      {edited && (
        <>
          {" · "}
          <Link href={`/questions/${questionId}/versions`} className="underline hover:text-text-primary">
            edited {relativeTime(updatedAt)} · revision {versionNumber}
          </Link>
        </>
      )}
    </p>
  );
}
