"use client";

import { useQuery } from "@tanstack/react-query";
import { tagApi } from "../api/tag.api";
import { tagKeys } from "../api/tag.keys";

export function useTagSearch(q: string) {
  return useQuery({
    queryKey: tagKeys.search(q),
    queryFn: () => tagApi.search(q || undefined),
  });
}
