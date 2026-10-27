"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { stackApi } from "../api/stack.api";
import { stackKeys } from "../api/stack.keys";
import type { StackDependency } from "../api/stack.types";
import { watchApi } from "@/features/watch/api/watch.api";
import { watchKeys } from "@/features/watch/api/watch.keys";

export function useMyStack(enabled: boolean) {
  return useQuery({
    queryKey: stackKeys.mine,
    queryFn: async () => (await stackApi.myStack()).dependencies,
    enabled,
  });
}

export function useReplaceMyStack() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (dependencies: StackDependency[]) => (await stackApi.replaceMyStack(dependencies)).dependencies,
    onSuccess: (dependencies) => {
      queryClient.setQueryData(stackKeys.mine, dependencies);
      queryClient.invalidateQueries({ queryKey: stackKeys.wardSuggestions });
    },
  });
}

export function useStackWardSuggestions(enabled: boolean) {
  return useQuery({
    queryKey: stackKeys.wardSuggestions,
    queryFn: stackApi.wardSuggestions,
    enabled,
  });
}

/** Watches only the suggestions the user ticked — the opt-in of ADR-0078. */
export function useWatchSuggestions() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (questionIds: number[]) => Promise.all(questionIds.map((id) => watchApi.watch(id))),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: watchKeys.mine });
      queryClient.invalidateQueries({ queryKey: stackKeys.wardSuggestions });
    },
  });
}
