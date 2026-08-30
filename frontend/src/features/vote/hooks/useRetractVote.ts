"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { voteApi } from "../api/vote.api";
import { voteKeys } from "../api/vote.keys";
import type { VoteTargetType } from "../api/vote.types";
import { questionKeys } from "@/features/question/api/question.keys";
import { answerKeys } from "@/features/answer/api/answer.keys";

export function useRetractVote(targetType: VoteTargetType, targetId: number, questionId: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () => voteApi.retract(targetType, targetId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: voteKeys.mine });
      if (targetType === "QUESTION") {
        queryClient.invalidateQueries({ queryKey: questionKeys.detail(questionId) });
      } else {
        queryClient.invalidateQueries({ queryKey: answerKeys.list(questionId) });
      }
    },
  });
}
