import type { TagQuestionSort } from "../model/tag.types";

export const tagKeys = {
  all: ["tags"] as const,
  search: (q: string) => [...tagKeys.all, "search", q] as const,
  byName: (name: string) => [...tagKeys.all, "by-name", name] as const,
  detail: (id: number) => [...tagKeys.all, id] as const,
  questions: (id: number, sort: TagQuestionSort) => [...tagKeys.all, id, "questions", sort] as const,
  contributors: (id: number) => [...tagKeys.all, id, "contributors"] as const,
  related: (id: number) => [...tagKeys.all, id, "related"] as const,
};
