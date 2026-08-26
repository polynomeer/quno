"use client";

import { useQuery } from "@tanstack/react-query";
import { questionApi } from "../api/question.api";
import { questionKeys } from "../api/question.keys";

export function useRelatedQuestions(id: number, limit = 5) {
  return useQuery({
    queryKey: questionKeys.related(id),
    queryFn: () => questionApi.related(id, limit),
  });
}
