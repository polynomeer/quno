"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { answerApi } from "../api/answer.api";
import { answerKeys } from "../api/answer.keys";

export function useReviseAnswer(answerId: number, questionId: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (body: string) => answerApi.revise(answerId, body),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: answerKeys.list(questionId) });
      queryClient.invalidateQueries({ queryKey: answerKeys.versions(answerId) });
    },
  });
}
