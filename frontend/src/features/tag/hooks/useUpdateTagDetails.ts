"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { tagApi } from "@/entities/tag/api/tag.api";
import { tagKeys } from "@/entities/tag/api/tag.keys";

export function useUpdateTagDetails(tagId: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (input: { description?: string; docsUrl?: string }) => tagApi.updateDetails(tagId, input),
    onSuccess: (tag) => {
      queryClient.setQueryData(tagKeys.detail(tagId), tag);
      queryClient.invalidateQueries({ queryKey: tagKeys.byName(tag.name) });
    },
  });
}
