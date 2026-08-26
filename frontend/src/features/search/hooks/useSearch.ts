"use client";

import { useQuery } from "@tanstack/react-query";
import { searchApi } from "../api/search.api";
import { searchKeys } from "../api/search.keys";

export function useSearch(q: string) {
  return useQuery({
    queryKey: searchKeys.results(q),
    queryFn: () => searchApi.search(q),
    enabled: q.trim().length > 0,
  });
}
