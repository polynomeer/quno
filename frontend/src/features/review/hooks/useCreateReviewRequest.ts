"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { reviewApi } from "../api/review.api";
import { reviewKeys } from "../api/review.keys";
import { questionKeys } from "@/features/question/api/question.keys";

/** Opening a request also flips the question to NEEDS_INFO — invalidate its detail too. */
export function useCreateReviewRequest(questionId: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (message: string) => reviewApi.create(questionId, message),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: reviewKeys.list(questionId) });
      queryClient.invalidateQueries({ queryKey: questionKeys.detail(questionId) });
    },
  });
}
