import type { CommentTargetType } from "./comment.types";

export const commentKeys = {
  list: (targetType: CommentTargetType, targetId: number) => ["comments", targetType, targetId] as const,
};
