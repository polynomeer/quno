import Link from "next/link";
import { MarkdownContent } from "@/shared/ui/MarkdownContent";
import { Button } from "@/shared/ui/Button";
import { relativeTime } from "@/shared/lib/relative-time";
import { cn } from "@/shared/lib/cn";
import { VoteControl } from "@/features/vote/ui/VoteControl";
import { CommentSection } from "@/features/comment/ui/CommentSection";
import type { Answer } from "../api/answer.types";

export function AnswerCard({
  answer,
  canAccept,
  onAccept,
  isAccepting,
  questionHref,
  showEngagement,
}: {
  answer: Answer;
  /** Only the question's author can accept (backend: QuestionAccessDeniedException otherwise). */
  canAccept?: boolean;
  onAccept?: () => void;
  isAccepting?: boolean;
  /** Shown above the body when this card is listed outside its question's own page (e.g. a profile). */
  questionHref?: string;
  /** Vote/Comment need the card's own question page context (score invalidation, comment target) —
   * off by default so profile-page answer lists stay read-only. */
  showEngagement?: boolean;
}) {
  return (
    <li
      id={`answer-${answer.id}`}
      className={cn(
        "scroll-mt-20 rounded-lg border p-4",
        answer.isAccepted ? "border-success bg-success-subtle/30" : "border-border",
      )}
    >
      {questionHref && (
        <Link href={questionHref} className="mb-2 block text-xs text-text-secondary hover:underline">
          질문 보기 →
        </Link>
      )}
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
        {canAccept && !answer.isAccepted && (
          <Button variant="secondary" className="ml-auto px-2 py-1 text-xs" onClick={onAccept} disabled={isAccepting}>
            {isAccepting ? "채택 중..." : "Accept"}
          </Button>
        )}
      </div>
      <div className="flex gap-4">
        {showEngagement ? (
          <VoteControl
            targetType="ANSWER"
            targetId={answer.id}
            questionId={answer.questionId}
            score={answer.score}
            authorId={answer.authorId}
          />
        ) : (
          <span className="text-sm font-semibold text-text-secondary">{answer.score}</span>
        )}
        <div className="flex-1">
          <MarkdownContent>{answer.body}</MarkdownContent>
          {showEngagement && <CommentSection targetType="ANSWER" targetId={answer.id} />}
        </div>
      </div>
    </li>
  );
}
