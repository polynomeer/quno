"use client";

import { useQuery } from "@tanstack/react-query";
import { answerApi } from "../api/answer.api";
import { answerKeys } from "../api/answer.keys";

export function useAnswerDiff(answerId: number, version: number, from: number, enabled: boolean) {
  return useQuery({
    queryKey: answerKeys.diff(answerId, version, from),
    queryFn: () => answerApi.getDiff(answerId, version, from),
    enabled,
  });
}
