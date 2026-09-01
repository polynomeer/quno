import { httpClient } from "@/shared/api/http-client";
import type { QuestionSummary } from "@/features/question/api/question.types";
import type { Tag, TagContributor, TagQuestionSort } from "../model/tag.types";

export const tagApi = {
  search: (q?: string) => {
    const query = q ? `?q=${encodeURIComponent(q)}` : "";
    return httpClient.get<Tag[]>(`/api/v1/tags${query}`);
  },
  get: (id: number) => httpClient.get<Tag>(`/api/v1/tags/${id}`),
  updateDetails: (id: number, input: { description?: string; docsUrl?: string }) =>
    httpClient.put<Tag>(`/api/v1/tags/${id}`, input),
  questions: (id: number, sort: TagQuestionSort, limit = 20) =>
    httpClient.get<QuestionSummary[]>(`/api/v1/tags/${id}/questions?sort=${sort}&limit=${limit}`),
  contributors: (id: number, limit = 10) =>
    httpClient.get<TagContributor[]>(`/api/v1/tags/${id}/contributors?limit=${limit}`),
  related: (id: number, limit = 10) => httpClient.get<Tag[]>(`/api/v1/tags/${id}/related?limit=${limit}`),
  follow: (id: number) => httpClient.post<void>(`/api/v1/tags/${id}/follow`),
  unfollow: (id: number) => httpClient.delete<void>(`/api/v1/tags/${id}/follow`),
};
