"use client";

import { useQuery } from "@tanstack/react-query";
import { searchApi } from "../api/search.api";
import { searchKeys } from "../api/search.keys";
import type { SearchSort } from "../api/search.types";

export function useSearch(q: string, sort: SearchSort = "relevance") {
  return useQuery({
    queryKey: searchKeys.results(q, sort),
    queryFn: () => searchApi.search(q, 20, sort),
    enabled: q.trim().length > 0,
  });
}
