"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { answerApi } from "../api/answer.api";
import { answerKeys } from "../api/answer.keys";
import { questionKeys } from "@/features/question/api/question.keys";
import type { ApplicabilityRange } from "@/features/stack/api/stack.types";

export function useCreateAnswer(questionId: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ body, applicability }: { body: string; applicability: ApplicabilityRange[] }) =>
      applicability.length > 0 ? answerApi.create(questionId, body, { applicability }) : answerApi.create(questionId, body),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: answerKeys.list(questionId) });
      queryClient.invalidateQueries({ queryKey: questionKeys.timeline(questionId) });
    },
  });
}
