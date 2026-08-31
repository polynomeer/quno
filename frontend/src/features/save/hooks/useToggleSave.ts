"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { saveApi } from "../api/save.api";
import { saveKeys } from "../api/save.keys";

export function useToggleSave(questionId: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (isSaved: boolean) => (isSaved ? saveApi.unsave(questionId) : saveApi.save(questionId)),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: saveKeys.mine });
    },
  });
}
