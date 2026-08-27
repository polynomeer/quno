"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { clusterApi } from "../api/cluster.api";
import { clusterKeys } from "../api/cluster.keys";

export function useDesignateSuperAnswer(questionId: number, clusterId: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (answerId: number) => clusterApi.designateSuperAnswer(clusterId, answerId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: clusterKeys.forQuestion(questionId) });
    },
  });
}
