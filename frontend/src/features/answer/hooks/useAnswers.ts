"use client";

import { useQuery } from "@tanstack/react-query";
import { answerApi } from "../api/answer.api";
import { answerKeys } from "../api/answer.keys";

export function useAnswers(questionId: number) {
  return useQuery({
    queryKey: answerKeys.list(questionId),
    queryFn: () => answerApi.list(questionId),
  });
}
