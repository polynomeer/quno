"use client";

import { useQuery } from "@tanstack/react-query";
import { tagApi } from "../api/tag.api";
import { tagKeys } from "../api/tag.keys";

export function useTagContributors(tagId: number | null) {
  return useQuery({
    queryKey: tagKeys.contributors(tagId ?? 0),
    queryFn: () => tagApi.contributors(tagId as number),
    enabled: tagId !== null,
  });
}
