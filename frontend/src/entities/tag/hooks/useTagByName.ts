"use client";

import { useQuery } from "@tanstack/react-query";
import { tagApi } from "../api/tag.api";
import { tagKeys } from "../api/tag.keys";

/** There's no "get by name" endpoint, only search + numeric id lookup — mirrors how ADR-0021's
 * Tag Detail approximation matched exact names out of search results, just now to resolve an id
 * instead of filtering questions directly. */
export function useTagByName(name: string) {
  return useQuery({
    queryKey: tagKeys.byName(name),
    queryFn: async () => {
      const results = await tagApi.search(name);
      return results.find((tag) => tag.name === name) ?? null;
    },
  });
}
