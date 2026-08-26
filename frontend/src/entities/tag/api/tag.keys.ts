export const tagKeys = {
  all: ["tags"] as const,
  search: (q: string) => [...tagKeys.all, "search", q] as const,
};
