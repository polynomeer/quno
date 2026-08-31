"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { moderationApi } from "../api/moderation.api";
import { moderationKeys } from "../api/moderation.keys";

export function useDismissReport() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (reportId: number) => moderationApi.dismiss(reportId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: moderationKeys.reports("PENDING") });
    },
  });
}
