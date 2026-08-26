"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { answerApi } from "../api/answer.api";
import { answerKeys } from "../api/answer.keys";

export function useCreateAnswer(questionId: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (body: string) => answerApi.create(questionId, body),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: answerKeys.list(questionId) });
    },
  });
}
