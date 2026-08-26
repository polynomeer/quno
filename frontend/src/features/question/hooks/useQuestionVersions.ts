"use client";

import { useQuery } from "@tanstack/react-query";
import { questionApi } from "../api/question.api";
import { questionKeys } from "../api/question.keys";

export function useQuestionVersions(id: number) {
  return useQuery({
    queryKey: questionKeys.versions(id),
    queryFn: () => questionApi.listVersions(id),
  });
}
