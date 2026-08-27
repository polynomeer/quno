"use client";

import { useQuery } from "@tanstack/react-query";
import { reviewApi } from "../api/review.api";
import { reviewKeys } from "../api/review.keys";

export function useReviewRequests(questionId: number) {
  return useQuery({
    queryKey: reviewKeys.list(questionId),
    queryFn: () => reviewApi.list(questionId),
  });
}
