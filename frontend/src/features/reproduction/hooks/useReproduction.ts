"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { reproductionApi, reproductionKeys } from "../api/reproduction.api";
import type { AttachCapsuleInput } from "../api/reproduction.types";

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

export function useAttachCapsule(questionId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: AttachCapsuleInput) => reproductionApi.attach(questionId, input),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: reproductionKeys.panel(questionId) }),
  });
}

export function useRequestRerun(questionId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (reason: string) => reproductionApi.requestRerun(questionId, reason),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: reproductionKeys.panel(questionId) }),
  });
}
