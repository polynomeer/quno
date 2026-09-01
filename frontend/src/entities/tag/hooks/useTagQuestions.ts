"use client";

import { useQuery } from "@tanstack/react-query";
import { tagApi } from "../api/tag.api";
import { tagKeys } from "../api/tag.keys";
import type { TagQuestionSort } from "../model/tag.types";

export function useTagQuestions(tagId: number | null, sort: TagQuestionSort) {
  return useQuery({
    queryKey: tagKeys.questions(tagId ?? 0, sort),
    queryFn: () => tagApi.questions(tagId as number, sort),
    enabled: tagId !== null,
  });
}
