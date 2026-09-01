"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { directAskApi } from "../api/direct-ask.api";
import { directAskKeys } from "../api/direct-ask.keys";

export function useCreateDirectAsk() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ questionId, targetUserId, message }: { questionId: number; targetUserId: number; message?: string }) =>
      directAskApi.create(questionId, targetUserId, message),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: directAskKeys.mine("sent") });
    },
  });
}
