"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { commentApi } from "../api/comment.api";
import { commentKeys } from "../api/comment.keys";
import type { CommentTargetType } from "../api/comment.types";

export function useEditComment(targetType: CommentTargetType, targetId: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ commentId, body }: { commentId: number; body: string }) => commentApi.edit(commentId, body),
    onSuccess: (_result, { commentId }) => {
      queryClient.invalidateQueries({ queryKey: commentKeys.list(targetType, targetId) });
      queryClient.invalidateQueries({ queryKey: commentKeys.versions(commentId) });
    },
  });
}
