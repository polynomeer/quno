"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { answerApi } from "../api/answer.api";
import { answerKeys } from "../api/answer.keys";
import type { ApplicabilityRange } from "@/features/stack/api/stack.types";

export function useReviseAnswer(answerId: number, questionId: number) {
  const queryClient = useQueryClient();

  return useMutation({
    /** [applicability] undefined keeps the current ranges. */
    mutationFn: ({ body, applicability }: { body: string; applicability?: ApplicabilityRange[] }) =>
      applicability === undefined ? answerApi.revise(answerId, body) : answerApi.revise(answerId, body, applicability),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: answerKeys.list(questionId) });
      queryClient.invalidateQueries({ queryKey: answerKeys.versions(answerId) });
    },
  });
}
