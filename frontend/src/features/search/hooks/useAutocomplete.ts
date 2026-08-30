"use client";

import { useQuery } from "@tanstack/react-query";
import { searchApi } from "../api/search.api";
import { tagApi } from "@/entities/tag/api/tag.api";
import { useDebouncedValue } from "@/shared/hooks/useDebouncedValue";

const MIN_LENGTH = 2;

/** design.md #10 "검색어 자동완성: 질문 제목, 태그" — combines both into one dropdown. */
export function useAutocomplete(query: string) {
  const debounced = useDebouncedValue(query.trim(), 250);
  const enabled = debounced.length >= MIN_LENGTH;

  const { data: questions, isLoading: questionsLoading } = useQuery({
    queryKey: ["autocomplete", "questions", debounced],
    queryFn: () => searchApi.search(debounced, 5),
    enabled,
  });

  const { data: tags, isLoading: tagsLoading } = useQuery({
    queryKey: ["autocomplete", "tags", debounced],
    queryFn: () => tagApi.search(debounced),
    enabled,
  });

  return {
    enabled,
    isLoading: questionsLoading || tagsLoading,
    questions: questions ?? [],
    tags: (tags ?? []).slice(0, 5),
  };
}
