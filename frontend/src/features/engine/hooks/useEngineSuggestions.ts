"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { engineApi, engineKeys } from "../api/engine.api";
import { questionKeys } from "@/features/question/api/question.keys";

export function useEngineSuggestions(questionId: number, enabled: boolean) {
  return useQuery({ queryKey: engineKeys.suggestions(questionId), queryFn: () => engineApi.list(questionId), enabled });
}

/** Accepting makes a revision, so the question, its versions and timeline all change too. */
export function useResolveEngineSuggestion(questionId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, action }: { id: number; action: "accept" | "dismiss" }) =>
      action === "accept" ? engineApi.accept(id) : engineApi.dismiss(id),
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: engineKeys.suggestions(questionId) });
      queryClient.invalidateQueries({ queryKey: questionKeys.detail(questionId) });
      queryClient.invalidateQueries({ queryKey: questionKeys.versions(questionId) });
      queryClient.invalidateQueries({ queryKey: questionKeys.timeline(questionId) });
    },
  });
}
