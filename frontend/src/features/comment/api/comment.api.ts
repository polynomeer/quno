import { httpClient } from "@/shared/api/http-client";
import type { Comment, CommentTargetType, CommentVersion } from "./comment.types";

function commentsPath(targetType: CommentTargetType, targetId: number): string {
  return targetType === "QUESTION" ? `/api/v1/questions/${targetId}/comments` : `/api/v1/answers/${targetId}/comments`;
}

export const commentApi = {
  list: (targetType: CommentTargetType, targetId: number) => httpClient.get<Comment[]>(commentsPath(targetType, targetId)),
  create: (targetType: CommentTargetType, targetId: number, body: string, parentCommentId?: number) =>
    httpClient.post<Comment>(commentsPath(targetType, targetId), { body, parentCommentId }),
  edit: (commentId: number, body: string) => httpClient.put<Comment>(`/api/v1/comments/${commentId}`, { body }),
  getVersions: (commentId: number) => httpClient.get<CommentVersion[]>(`/api/v1/comments/${commentId}/versions`),
  remove: (commentId: number) => httpClient.delete<void>(`/api/v1/comments/${commentId}`),
};
