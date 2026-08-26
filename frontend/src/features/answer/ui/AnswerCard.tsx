import { MarkdownContent } from "@/shared/ui/MarkdownContent";
import { relativeTime } from "@/shared/lib/relative-time";
import { cn } from "@/shared/lib/cn";
import type { Answer } from "../api/answer.types";

export function AnswerCard({ answer }: { answer: Answer }) {
  return (
    <li
      className={cn(
        "rounded-lg border p-4",
        answer.isAccepted ? "border-success bg-success-subtle/30" : "border-border",
      )}
    >
      <div className="mb-2 flex flex-wrap items-center gap-2 text-xs text-text-secondary">
        {answer.isAccepted && (
          <span className="inline-flex items-center rounded-full bg-success-subtle px-2 py-0.5 font-medium text-success">
            ✓ Accepted
          </span>
        )}
        {answer.isStale && (
          <span className="inline-flex items-center rounded-full bg-warning-subtle px-2 py-0.5 font-medium text-warning">
            질문이 이후 수정됨 (v{answer.targetVersionNumber} 기준 답변)
          </span>
        )}
        <span>사용자 #{answer.authorId}</span>
        <span>· {relativeTime(answer.createdAt)}</span>
      </div>
      <MarkdownContent>{answer.body}</MarkdownContent>
    </li>
  );
}
