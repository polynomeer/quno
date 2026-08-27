import { httpClient } from "@/shared/api/http-client";
import type { ReviewRequest } from "./review.types";

export const reviewApi = {
  list: (questionId: number) => httpClient.get<ReviewRequest[]>(`/api/v1/questions/${questionId}/review-requests`),
  create: (questionId: number, message: string) =>
    httpClient.post<ReviewRequest>(`/api/v1/questions/${questionId}/review-requests`, { message }),
  reRequest: (questionId: number, reviewRequestId: number) =>
    httpClient.post<ReviewRequest>(`/api/v1/questions/${questionId}/review-requests/${reviewRequestId}/re-request`),
};
