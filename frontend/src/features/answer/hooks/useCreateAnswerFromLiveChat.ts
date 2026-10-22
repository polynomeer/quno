"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { answerApi } from "../api/answer.api";
import { answerKeys } from "../api/answer.keys";
import { questionKeys } from "@/features/question/api/question.keys";

/** Publishes an answer that cites the live chat it came from (Phase 71, ADR-0072). */
export function useCreateAnswerFromLiveChat(
  questionId: number,
  liveChatRoomId: number | null,
) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (body: string) =>
      answerApi.create(questionId, body, liveChatRoomId ?? undefined),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: answerKeys.list(questionId) });
      queryClient.invalidateQueries({
        queryKey: questionKeys.timeline(questionId),
      });
    },
  });
}
