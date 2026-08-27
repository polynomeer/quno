"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { questionApi } from "../api/question.api";
import { questionKeys } from "../api/question.keys";

export function useMarkOutdated(questionId: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (reason: string) => questionApi.markOutdated(questionId, reason),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: questionKeys.detail(questionId) });
    },
  });
}
