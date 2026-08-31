import type { SearchSort } from "./search.types";

export const searchKeys = {
  results: (q: string, sort: SearchSort) => ["search", q, sort] as const,
};
