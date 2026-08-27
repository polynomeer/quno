"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { clusterApi } from "../api/cluster.api";
import { clusterKeys } from "../api/cluster.keys";

export function useMarkAsSameProblem(questionId: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (relatedQuestionId: number) => clusterApi.markAsSameProblem(questionId, relatedQuestionId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: clusterKeys.forQuestion(questionId) });
    },
  });
}
