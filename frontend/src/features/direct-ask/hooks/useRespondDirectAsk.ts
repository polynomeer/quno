"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { directAskApi } from "../api/direct-ask.api";
import { directAskKeys } from "../api/direct-ask.keys";

export function useRespondDirectAsk(id: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (accept: boolean) => (accept ? directAskApi.accept(id) : directAskApi.decline(id)),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: directAskKeys.mine("received") });
    },
  });
}
