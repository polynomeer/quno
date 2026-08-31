import { httpClient } from "@/shared/api/http-client";
import type { QuestionSummary } from "@/features/question/api/question.types";
import type { SearchSort } from "./search.types";

export const searchApi = {
  search: (q: string, limit = 20, sort: SearchSort = "relevance") =>
    httpClient.get<QuestionSummary[]>(
      `/api/v1/search?q=${encodeURIComponent(q)}&limit=${limit}&sort=${sort}`,
    ),
};
