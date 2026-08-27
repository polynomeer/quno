"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { reviewApi } from "../api/review.api";
import { reviewKeys } from "../api/review.keys";

/** Re-requesting only flips that ReviewRequest's own status — it never touches Question.status
 * (docs/architecture/api-design.md QPR §5.3), so no question-detail invalidation here. */
export function useReRequestReview(questionId: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (reviewRequestId: number) => reviewApi.reRequest(questionId, reviewRequestId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: reviewKeys.list(questionId) });
    },
  });
}
