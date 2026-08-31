"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { questionApi } from "../api/question.api";
import { questionKeys } from "../api/question.keys";

export function useForkQuestion(id: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () => questionApi.fork(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: questionKeys.graph(id) });
    },
  });
}
