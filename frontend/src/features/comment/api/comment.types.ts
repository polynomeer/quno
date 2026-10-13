export type CommentTargetType = "QUESTION" | "ANSWER";

export interface Comment {
  id: number;
  targetType: CommentTargetType;
  targetId: number;
  authorId: number;
  /** Set by the list endpoints (ADR-0064); null/absent on create and edit responses. */
  authorNickname?: string | null;
  parentCommentId: number | null;
  body: string | null;
  versionNumber: number;
  isDeleted: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CommentVersion {
  versionNumber: number;
  body: string;
  createdAt: string;
}

export const MAX_COMMENT_BODY_LENGTH = 600;
