"use client";

import { useQuery } from "@tanstack/react-query";
import { commentApi } from "../api/comment.api";
import { commentKeys } from "../api/comment.keys";

/** Only fetched once the user expands the inline "edited (vN)" history disclosure — pass
 * `enabled` so the versions endpoint isn't hit for every comment on the page. */
export function useCommentVersions(commentId: number, enabled: boolean) {
  return useQuery({
    queryKey: commentKeys.versions(commentId),
    queryFn: () => commentApi.getVersions(commentId),
    enabled,
  });
}
