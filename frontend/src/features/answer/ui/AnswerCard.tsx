"use client";

import { useState } from "react";
import Link from "next/link";
import { MarkdownContent } from "@/shared/ui/MarkdownContent";
import { MarkdownEditor } from "@/shared/ui/MarkdownEditor";
import { Button } from "@/shared/ui/Button";
import { relativeTime } from "@/shared/lib/relative-time";
import { userLabel } from "@/shared/lib/user-label";
import { cn } from "@/shared/lib/cn";
import { VoteControl } from "@/features/vote/ui/VoteControl";
import { CommentSection } from "@/features/comment/ui/CommentSection";
import { ReportButton } from "@/features/report/ui/ReportButton";
import { useSession } from "@/features/auth/hooks/useSession";
import { useAnswerVersions } from "../hooks/useAnswerVersions";
import { useReviseAnswer } from "../hooks/useReviseAnswer";
import { FormError } from "@/shared/ui/FormError";
import type { Answer } from "../api/answer.types";
import type { StackDependency } from "@/features/stack/api/stack.types";
import {
  type ApplicabilityRow,
  rowFromRange,
  rowsToRanges,
  sameRanges,
} from "@/features/stack/lib/applicability";
import { ApplicabilityChips } from "@/features/stack/ui/ApplicabilityChips";
import { ApplicabilityRangeEditor } from "@/features/stack/ui/ApplicabilityRangeEditor";

export function AnswerCard({
  answer,
  canAccept,
  onAccept,
  isAccepting,
  questionHref,
  showEngagement,
  questionDependencies,
}: {
  answer: Answer;
  /** Only the question's author can accept (backend: QuestionAccessDeniedException otherwise). */
  canAccept?: boolean;
  onAccept?: () => void;
  isAccepting?: boolean;
  /** Shown above the body when this card is listed outside its question's own page (e.g. a profile). */
  questionHref?: string;
  /** Vote/Comment/Edit/Report need the card's own question page context — off by default so
   * profile-page answer lists stay read-only. */
  showEngagement?: boolean;
  /** Offered as applicability rows while editing (ADR-0077). */
  questionDependencies?: StackDependency[];
}) {
  const { data: me } = useSession();
  const { data: versions } = useAnswerVersions(
    answer.id,
    Boolean(showEngagement),
  );
  const reviseAnswer = useReviseAnswer(answer.id, answer.questionId);
  const [isEditing, setIsEditing] = useState(false);
  const [draft, setDraft] = useState(answer.body);
  const ranges = answer.applicability ?? [];
  const [rows, setRows] = useState<ApplicabilityRow[]>([]);

  const isAuthor = Boolean(showEngagement && me && me.id === answer.authorId);
  const latestVersionNumber =
    versions && versions.length > 0
      ? Math.max(...versions.map((v) => v.versionNumber))
      : null;
  const isEdited = latestVersionNumber !== null && latestVersionNumber > 1;

  async function handleSave() {
    if (!draft.trim()) return;
    try {
      const edited = rowsToRanges(rows);
      // Unchanged ranges are omitted so the revision keeps them as they are (ADR-0077).
      await reviseAnswer.mutateAsync({
        body: draft.trim(),
        applicability: sameRanges(edited, ranges) ? undefined : edited,
      });
      setIsEditing(false);
    } catch {
      // error surfaced below via reviseAnswer.error
    }
  }

  return (
    <li
      id={`answer-${answer.id}`}
      className={cn(
        "scroll-mt-28 rounded-3xl border p-5 sm:px-7 sm:py-6",
        // Accepted gets a light success edge, never a fill that drowns out the other answers
        // (design.md #13.1, design canvas ADR-0061).
        answer.isAccepted
          ? "border-[1.5px] border-success-border bg-surface"
          : "border-border bg-surface",
      )}
    >
      {questionHref && (
        <Link
          href={questionHref}
          className="mb-2 block text-xs text-text-secondary hover:underline"
        >
          질문 보기 →
        </Link>
      )}
      <div className="mb-2 flex flex-wrap items-center gap-2 text-xs text-text-secondary">
        {answer.isAccepted && (
          <span className="inline-flex h-[22px] items-center rounded-full bg-success-subtle px-2.5 font-semibold text-success">
            ✓ Accepted
          </span>
        )}
        {answer.sourceLiveChatRoomId != null && (
          // Provenance from Phase 71 (ADR-0073): the room never closes, so the link stays valid.
          <Link
            href={`/questions/${answer.questionId}#live-chat`}
            className="inline-flex h-[22px] items-center rounded-full bg-live-ring px-2.5 font-semibold text-success hover:underline"
          >
            질문방 대화에서 정리됨
          </Link>
        )}
        {answer.isStale && (
          <Link
            href={`/questions/${answer.questionId}/versions`}
            className="inline-flex h-[22px] items-center rounded-full bg-warning-subtle px-2.5 font-semibold text-warning hover:underline"
          >
            질문이 이후 수정됨 (v{answer.targetVersionNumber} 기준 답변)
          </Link>
        )}
        <Link
          href={`/users/${answer.authorId}`}
          className="font-medium text-text-primary hover:text-brand"
        >
          {userLabel(answer.authorId, answer.authorNickname)}
        </Link>
        <span>· {relativeTime(answer.createdAt)}</span>
        {isEdited && (
          <Link
            href={`/answers/${answer.id}/versions?questionId=${answer.questionId}`}
            className="font-mono hover:text-text-primary hover:underline"
          >
            edited · revision {latestVersionNumber}
          </Link>
        )}
        {isAuthor && !isEditing && (
          <button
            type="button"
            onClick={() => {
              setDraft(answer.body);
              setRows(ranges.map(rowFromRange));
              setIsEditing(true);
            }}
            className="hover:text-text-primary"
          >
            Edit
          </button>
        )}
        {showEngagement && !isAuthor && (
          <ReportButton targetType="ANSWER" targetId={answer.id} />
        )}
        {canAccept && !answer.isAccepted && (
          <Button
            variant="secondary"
            className="ml-auto px-2 py-1 text-xs"
            onClick={onAccept}
            disabled={isAccepting}
          >
            {isAccepting ? "채택 중..." : "Accept"}
          </Button>
        )}
      </div>
      <div className="flex gap-4 sm:gap-5">
        {showEngagement ? (
          <VoteControl
            targetType="ANSWER"
            targetId={answer.id}
            questionId={answer.questionId}
            score={answer.score}
            authorId={answer.authorId}
          />
        ) : (
          <span className="w-11 shrink-0 text-center font-mono text-lg font-semibold text-text-primary">
            {answer.score}
          </span>
        )}
        <div className="min-w-0 flex-1">
          {isEditing ? (
            <div className="space-y-2">
              <MarkdownEditor value={draft} onChange={setDraft} rows={6} />
              <ApplicabilityRangeEditor
                rows={rows}
                onChange={setRows}
                suggestions={questionDependencies}
                idPrefix={`answer-${answer.id}`}
              />
              <FormError
                error={reviseAnswer.error}
                fallback="수정하지 못했습니다."
              />
              <div className="flex gap-2">
                <Button
                  className="px-2 py-1 text-xs"
                  onClick={handleSave}
                  disabled={reviseAnswer.isPending || !draft.trim()}
                >
                  {reviseAnswer.isPending ? "저장 중..." : "Save"}
                </Button>
                <Button
                  variant="ghost"
                  className="px-2 py-1 text-xs"
                  onClick={() => setIsEditing(false)}
                >
                  Cancel
                </Button>
              </div>
            </div>
          ) : (
            <div className="space-y-3">
              <MarkdownContent>{answer.body}</MarkdownContent>
              <ApplicabilityChips ranges={ranges} />
            </div>
          )}
          {showEngagement && (
            <CommentSection targetType="ANSWER" targetId={answer.id} />
          )}
        </div>
      </div>
    </li>
  );
}
