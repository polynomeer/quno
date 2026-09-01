"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { directAskApi } from "../api/direct-ask.api";
import { directAskKeys } from "../api/direct-ask.keys";

export function useConfirmDirectAskPayment() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ orderId, paymentKey, amount }: { orderId: string; paymentKey: string; amount: number }) =>
      directAskApi.confirmPayment(orderId, paymentKey, amount),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: directAskKeys.mine("sent") });
    },
  });
}
