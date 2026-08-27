import { httpClient } from "@/shared/api/http-client";
import type {
  CreateQuestionInput,
  QuestionDetail,
  QuestionMutationResult,
  QuestionSummary,
  QuestionVersionDetail,
  QuestionVersionDiff,
  QuestionVersionSummary,
} from "./question.types";

export const questionApi = {
  create: (input: CreateQuestionInput) => httpClient.post<QuestionMutationResult>("/api/v1/questions", input),
  get: (id: number) => httpClient.get<QuestionDetail>(`/api/v1/questions/${id}`),
  listVersions: (id: number) => httpClient.get<QuestionVersionSummary[]>(`/api/v1/questions/${id}/versions`),
  getVersion: (id: number, version: number) =>
    httpClient.get<QuestionVersionDetail>(`/api/v1/questions/${id}/versions/${version}`),
  getDiff: (id: number, version: number, from?: number) => {
    const query = from !== undefined ? `?from=${from}` : "";
    return httpClient.get<QuestionVersionDiff>(`/api/v1/questions/${id}/versions/${version}/diff${query}`);
  },
  related: (id: number, limit = 5) =>
    httpClient.get<QuestionSummary[]>(`/api/v1/questions/${id}/related?limit=${limit}`),
  markOutdated: (id: number, reason: string) =>
    httpClient.post<QuestionMutationResult>(`/api/v1/questions/${id}/outdated`, { reason }),
};
