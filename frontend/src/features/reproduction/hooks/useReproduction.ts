"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { reproductionApi, reproductionKeys } from "../api/reproduction.api";
import type { AttachCapsuleInput } from "../api/reproduction.types";
import { verificationKeys } from "@/features/verification/api/verification.api";

export function useReproductionPanel(questionId: number) {
  return useQuery({
    queryKey: reproductionKeys.panel(questionId),
    queryFn: () => reproductionApi.panel(questionId),
  });
}

export function useReproductionRunLog(questionId: number, runId: number, enabled: boolean) {
  return useQuery({
    queryKey: reproductionKeys.log(questionId, runId),
    queryFn: () => reproductionApi.log(questionId, runId),
    enabled,
  });
}

export function useReproductionComparison(questionId: number, runId: number, enabled: boolean) {
  return useQuery({
    queryKey: reproductionKeys.compare(questionId, runId),
    queryFn: () => reproductionApi.compare(questionId, runId),
    enabled,
  });
}

/** Every search action changes the panel, and the card reads an approved boundary. */
export function useBisectActions(questionId: number) {
  const queryClient = useQueryClient();
  const onSuccess = () => queryClient.invalidateQueries({ queryKey: reproductionKeys.panel(questionId) });
  return {
    start: useMutation({ mutationFn: (range: { from: string; to: string }) => reproductionApi.startBisect(questionId, range.from, range.to), onSuccess }),
    cancel: useMutation({ mutationFn: (bisectId: number) => reproductionApi.cancelBisect(questionId, bisectId), onSuccess }),
    decide: useMutation({ mutationFn: (input: { bisectId: number; approve: boolean }) => reproductionApi.decideBisect(questionId, input.bisectId, input.approve), onSuccess }),
  };
}

export function useAttachCapsule(questionId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: AttachCapsuleInput) => reproductionApi.attach(questionId, input),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: reproductionKeys.panel(questionId) });
      // A new capsule changes the checks and can drop a level earned through the old one.
      queryClient.invalidateQueries({ queryKey: verificationKeys.detail(questionId) });
    },
  });
}

export function useRequestRerun(questionId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (reason: string) => reproductionApi.requestRerun(questionId, reason),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: reproductionKeys.panel(questionId) });
      queryClient.invalidateQueries({ queryKey: verificationKeys.detail(questionId) });
    },
  });
}
