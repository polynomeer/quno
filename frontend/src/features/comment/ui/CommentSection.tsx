"use client";

import { useState } from "react";
import { useSession } from "@/features/auth/hooks/useSession";
import { useComments } from "../hooks/useComments";
import { useCreateComment } from "../hooks/useCreateComment";
import { CommentItem } from "./CommentItem";
import { Textarea } from "@/shared/ui/Textarea";
import { Button } from "@/shared/ui/Button";
import { ApiError } from "@/shared/api/api-error";
import { MAX_COMMENT_BODY_LENGTH, type CommentTargetType } from "../api/comment.types";

/** Up to one level of reply nesting, editable with inline history, @mention highlighting
 * (ADR-0024, ADR-0031) — plain text, no markdown rendering. Composer is hidden behind an
 * "Add a comment" toggle so every card doesn't show an input by default. Reading comments works
 * for anonymous viewers too (Phase 29, ADR-0041); the toggle itself only shows when logged in. */
export function CommentSection({ targetType, targetId }: { targetType: CommentTargetType; targetId: number }) {
  const { data: me } = useSession();
  const { data: comments } = useComments(targetType, targetId);
  const createComment = useCreateComment(targetType, targetId);
  const [showComposer, setShowComposer] = useState(false);
  const [draft, setDraft] = useState("");

  function handleSubmit() {
    const body = draft.trim();
    if (!body) return;
    createComment.mutate(
      { body },
      {
        onSuccess: () => {
          setDraft("");
          setShowComposer(false);
        },
      },
    );
  }

  const topLevel = comments?.filter((c) => c.parentCommentId == null) ?? [];
  const repliesByParent = new Map<number, typeof topLevel>();
  for (const c of comments ?? []) {
    if (c.parentCommentId != null) {
      repliesByParent.set(c.parentCommentId, [...(repliesByParent.get(c.parentCommentId) ?? []), c]);
    }
  }

  return (
    <div className="mt-3 space-y-2 border-t border-border pt-2 text-sm">
      {topLevel.length > 0 && (
        <ul className="space-y-1.5">
          {topLevel.map((comment) => (
            <CommentItem key={comment.id} comment={comment} currentUserId={me?.id} replies={repliesByParent.get(comment.id)} />
          ))}
        </ul>
      )}

      {!me ? null : !showComposer ? (
        <button
          type="button"
          onClick={() => setShowComposer(true)}
          className="text-xs text-text-secondary hover:text-brand"
        >
          Add a comment
        </button>
      ) : (
        <div className="space-y-1.5">
          <Textarea
            value={draft}
            onChange={(event) => setDraft(event.target.value)}
            maxLength={MAX_COMMENT_BODY_LENGTH}
            rows={2}
            placeholder="댓글을 입력하세요"
            autoFocus
          />
          {createComment.isError && (
            <p className="text-xs text-danger">
              {createComment.error instanceof ApiError ? createComment.error.message : "댓글을 등록하지 못했습니다."}
            </p>
          )}
          <div className="flex items-center gap-2">
            <Button
              variant="secondary"
              className="px-2 py-1 text-xs"
              onClick={handleSubmit}
              disabled={createComment.isPending || !draft.trim()}
            >
              {createComment.isPending ? "등록 중..." : "Comment"}
            </Button>
            <button
              type="button"
              onClick={() => {
                setShowComposer(false);
                setDraft("");
              }}
              className="text-xs text-text-secondary hover:underline"
            >
              취소
            </button>
            <span className="ml-auto text-xs text-text-secondary">
              {draft.length}/{MAX_COMMENT_BODY_LENGTH}
            </span>
          </div>
        </div>
      )}
    </div>
  );
}
