"use client";

import { useQuery } from "@tanstack/react-query";
import { tagApi } from "../api/tag.api";
import { tagKeys } from "../api/tag.keys";

export function useRelatedTags(tagId: number | null) {
  return useQuery({
    queryKey: tagKeys.related(tagId ?? 0),
    queryFn: () => tagApi.related(tagId as number),
    enabled: tagId !== null,
  });
}
