"use client";

import { useQuery } from "@tanstack/react-query";
import { commentApi } from "../api/comment.api";
import { commentKeys } from "../api/comment.keys";
import type { CommentTargetType } from "../api/comment.types";

export function useComments(targetType: CommentTargetType, targetId: number) {
  return useQuery({
    queryKey: commentKeys.list(targetType, targetId),
    queryFn: () => commentApi.list(targetType, targetId),
  });
}
