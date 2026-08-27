"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { watchApi } from "../api/watch.api";
import { watchKeys } from "../api/watch.keys";

export function useToggleWatch(questionId: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (isWatching: boolean) => (isWatching ? watchApi.unwatch(questionId) : watchApi.watch(questionId)),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: watchKeys.mine });
    },
  });
}
