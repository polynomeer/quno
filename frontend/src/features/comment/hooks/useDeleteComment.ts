"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { commentApi } from "../api/comment.api";
import { commentKeys } from "../api/comment.keys";
import type { CommentTargetType } from "../api/comment.types";

export function useDeleteComment(targetType: CommentTargetType, targetId: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (commentId: number) => commentApi.remove(commentId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: commentKeys.list(targetType, targetId) });
    },
  });
}
