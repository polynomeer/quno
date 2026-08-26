import { httpClient } from "@/shared/api/http-client";
import type { QuestionSummary } from "@/features/question/api/question.types";

export const searchApi = {
  search: (q: string, limit = 20) =>
    httpClient.get<QuestionSummary[]>(`/api/v1/search?q=${encodeURIComponent(q)}&limit=${limit}`),
};
