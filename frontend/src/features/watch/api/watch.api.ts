import { httpClient } from "@/shared/api/http-client";
import type { WatchedQuestion } from "./watch.types";

export const watchApi = {
  myWatches: () => httpClient.get<WatchedQuestion[]>("/api/v1/me/watches"),
  watch: (questionId: number) => httpClient.post<void>(`/api/v1/questions/${questionId}/watch`),
  unwatch: (questionId: number) => httpClient.delete<void>(`/api/v1/questions/${questionId}/watch`),
};
