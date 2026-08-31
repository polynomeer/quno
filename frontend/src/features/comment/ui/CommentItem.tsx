"use client";

import { Fragment, useState } from "react";
import { relativeTime } from "@/shared/lib/relative-time";
import { Textarea } from "@/shared/ui/Textarea";
import { Button } from "@/shared/ui/Button";
import { ApiError } from "@/shared/api/api-error";
import { useDeleteComment } from "../hooks/useDeleteComment";
import { useEditComment } from "../hooks/useEditComment";
import { useCreateComment } from "../hooks/useCreateComment";
import { useCommentVersions } from "../hooks/useCommentVersions";
import { MAX_COMMENT_BODY_LENGTH, type Comment } from "../api/comment.types";

const MENTION_PATTERN = /@[\w-]+/g;

/** Highlights `@nickname` tokens — styling only, no profile link (ADR-0031 #3: the backend never
 * exposes which user a mention resolved to in read responses). */
function renderBodyWithMentions(body: string) {
  const parts = body.split(MENTION_PATTERN);
  const mentions = body.match(MENTION_PATTERN) ?? [];
  return parts.flatMap((part, i) => [
    <Fragment key={`t-${i}`}>{part}</Fragment>,
    mentions[i] ? (
      <span key={`m-${i}`} className="font-medium text-brand">
        {mentions[i]}
      </span>
    ) : null,
  ]);
}

export function CommentItem({
  comment,
  currentUserId,
  replies,
}: {
  comment: Comment;
  currentUserId?: number;
  /** Only passed for a top-level comment — replies themselves render without this (one level
   * of nesting only, ADR-0031 #1). */
  replies?: Comment[];
}) {
  const deleteComment = useDeleteComment(comment.targetType, comment.targetId);
  const editComment = useEditComment(comment.targetType, comment.targetId);
  const createComment = useCreateComment(comment.targetType, comment.targetId);

  const [isEditing, setIsEditing] = useState(false);
  const [editDraft, setEditDraft] = useState(comment.body ?? "");
  const [showHistory, setShowHistory] = useState(false);
  const [isReplying, setIsReplying] = useState(false);
  const [replyDraft, setReplyDraft] = useState("");

  const { data: versions } = useCommentVersions(comment.id, showHistory);

  const isAuthor = !comment.isDeleted && currentUserId === comment.authorId;
  const isEdited = comment.versionNumber > 1;
  const canReply = comment.parentCommentId == null;

  function handleEditSave() {
    const body = editDraft.trim();
    if (!body) return;
    editComment.mutate({ commentId: comment.id, body }, { onSuccess: () => setIsEditing(false) });
  }

  function handleReplySubmit() {
    const body = replyDraft.trim();
    if (!body) return;
    createComment.mutate(
      { body, parentCommentId: comment.id },
      { onSuccess: () => { setReplyDraft(""); setIsReplying(false); } },
    );
  }

  return (
    <li className="text-text-secondary">
      <div className="flex items-start justify-between gap-2">
        {isEditing ? (
          <div className="flex-1 space-y-1.5">
            <Textarea
              value={editDraft}
              onChange={(event) => setEditDraft(event.target.value)}
              maxLength={MAX_COMMENT_BODY_LENGTH}
              rows={2}
              autoFocus
            />
            {editComment.isError && (
              <p className="text-xs text-danger">
                {editComment.error instanceof ApiError ? editComment.error.message : "수정하지 못했습니다."}
              </p>
            )}
            <div className="flex items-center gap-2">
              <Button
                variant="secondary"
                className="px-2 py-1 text-xs"
                onClick={handleEditSave}
                disabled={editComment.isPending || !editDraft.trim()}
              >
                {editComment.isPending ? "저장 중..." : "저장"}
              </Button>
              <button type="button" onClick={() => setIsEditing(false)} className="text-xs hover:underline">
                취소
              </button>
            </div>
          </div>
        ) : (
          <p>
            {comment.isDeleted ? (
              <em className="text-text-secondary/70">삭제된 댓글입니다</em>
            ) : (
              <span className="text-text-primary">{renderBodyWithMentions(comment.body ?? "")}</span>
            )}{" "}
            <span className="text-xs">
              — 사용자 #{comment.authorId} · {relativeTime(comment.createdAt)}
              {isEdited && (
                <>
                  {" · "}
                  <button type="button" onClick={() => setShowHistory((v) => !v)} className="underline hover:text-text-primary">
                    edited (v{comment.versionNumber})
                  </button>
                </>
              )}
              {canReply && !comment.isDeleted && (
                <>
                  {" · "}
                  <button type="button" onClick={() => setIsReplying((v) => !v)} className="hover:text-text-primary">
                    답글
                  </button>
                </>
              )}
              {isAuthor && !isEditing && (
                <>
                  {" · "}
                  <button type="button" onClick={() => { setEditDraft(comment.body ?? ""); setIsEditing(true); }} className="hover:text-text-primary">
                    수정
                  </button>
                </>
              )}
              {isAuthor && (
                <>
                  {" · "}
                  <button
                    type="button"
                    onClick={() => deleteComment.mutate(comment.id)}
                    disabled={deleteComment.isPending}
                    className="hover:text-danger disabled:pointer-events-none"
                  >
                    삭제
                  </button>
                </>
              )}
            </span>
          </p>
        )}
      </div>

      {showHistory && (
        <ul className="ml-4 mt-1 space-y-1 border-l border-border pl-2 text-xs">
          {versions === undefined ? (
            <li>불러오는 중...</li>
          ) : versions.length === 0 ? (
            <li>이전 이력이 없습니다.</li>
          ) : (
            versions.map((version) => (
              <li key={version.versionNumber}>
                v{version.versionNumber}: {version.body} · {relativeTime(version.createdAt)}
              </li>
            ))
          )}
        </ul>
      )}

      {isReplying && (
        <div className="ml-4 mt-1.5 space-y-1.5 border-l border-border pl-2">
          <Textarea
            value={replyDraft}
            onChange={(event) => setReplyDraft(event.target.value)}
            maxLength={MAX_COMMENT_BODY_LENGTH}
            rows={2}
            placeholder="답글을 입력하세요"
            autoFocus
          />
          {createComment.isError && (
            <p className="text-xs text-danger">
              {createComment.error instanceof ApiError ? createComment.error.message : "답글을 등록하지 못했습니다."}
            </p>
          )}
          <div className="flex items-center gap-2">
            <Button
              variant="secondary"
              className="px-2 py-1 text-xs"
              onClick={handleReplySubmit}
              disabled={createComment.isPending || !replyDraft.trim()}
            >
              {createComment.isPending ? "등록 중..." : "답글"}
            </Button>
            <button type="button" onClick={() => { setIsReplying(false); setReplyDraft(""); }} className="text-xs hover:underline">
              취소
            </button>
          </div>
        </div>
      )}

      {replies && replies.length > 0 && (
        <ul className="ml-4 mt-1.5 space-y-1.5 border-l border-border pl-2">
          {replies.map((reply) => (
            <CommentItem key={reply.id} comment={reply} currentUserId={currentUserId} />
          ))}
        </ul>
      )}
    </li>
  );
}
