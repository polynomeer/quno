"use client";

import { relativeTime } from "@/shared/lib/relative-time";
import { useDeleteComment } from "../hooks/useDeleteComment";
import type { Comment } from "../api/comment.types";

export function CommentItem({ comment, currentUserId }: { comment: Comment; currentUserId?: number }) {
  const deleteComment = useDeleteComment(comment.targetType, comment.targetId);
  const canDelete = !comment.isDeleted && currentUserId === comment.authorId;

  return (
    <li className="flex items-start justify-between gap-2 text-text-secondary">
      <p>
        {comment.isDeleted ? (
          <em className="text-text-secondary/70">삭제된 댓글입니다</em>
        ) : (
          <span className="text-text-primary">{comment.body}</span>
        )}{" "}
        <span className="text-xs">
          — 사용자 #{comment.authorId} · {relativeTime(comment.createdAt)}
        </span>
      </p>
      {canDelete && (
        <button
          type="button"
          onClick={() => deleteComment.mutate(comment.id)}
          disabled={deleteComment.isPending}
          className="shrink-0 text-xs text-text-secondary hover:text-danger disabled:pointer-events-none"
        >
          삭제
        </button>
      )}
    </li>
  );
}
