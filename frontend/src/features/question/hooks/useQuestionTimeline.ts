"use client";

import { useQuery } from "@tanstack/react-query";
import { questionApi } from "../api/question.api";
import { questionKeys } from "../api/question.keys";

export function useQuestionTimeline(id: number) {
  return useQuery({
    queryKey: questionKeys.timeline(id),
    queryFn: () => questionApi.timeline(id),
  });
}
