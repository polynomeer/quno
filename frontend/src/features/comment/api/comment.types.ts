export type CommentTargetType = "QUESTION" | "ANSWER";

export interface Comment {
  id: number;
  targetType: CommentTargetType;
  targetId: number;
  authorId: number;
  body: string | null;
  isDeleted: boolean;
  createdAt: string;
  updatedAt: string;
}

export const MAX_COMMENT_BODY_LENGTH = 600;
