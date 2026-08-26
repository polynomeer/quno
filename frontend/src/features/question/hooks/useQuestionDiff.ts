"use client";

import { useQuery } from "@tanstack/react-query";
import { questionApi } from "../api/question.api";
import { questionKeys } from "../api/question.keys";

export function useQuestionDiff(id: number, version: number, from: number, enabled: boolean) {
  return useQuery({
    queryKey: questionKeys.diff(id, version, from),
    queryFn: () => questionApi.getDiff(id, version, from),
    enabled,
  });
}
