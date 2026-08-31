"use client";

import { useQuery } from "@tanstack/react-query";
import { answerApi } from "../api/answer.api";
import { answerKeys } from "../api/answer.keys";

export function useAnswerVersions(answerId: number, enabled: boolean) {
  return useQuery({
    queryKey: answerKeys.versions(answerId),
    queryFn: () => answerApi.listVersions(answerId),
    enabled,
  });
}
