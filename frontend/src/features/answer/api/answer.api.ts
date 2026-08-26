import { httpClient } from "@/shared/api/http-client";
import type { Answer } from "./answer.types";

export interface AcceptAnswerResult {
  questionId: number;
  answerId: number;
  questionStatus: string;
}

export const answerApi = {
  list: (questionId: number) => httpClient.get<Answer[]>(`/api/v1/questions/${questionId}/answers`),
  create: (questionId: number, body: string) =>
    httpClient.post<Answer>(`/api/v1/questions/${questionId}/answers`, { body }),
  accept: (answerId: number) => httpClient.post<AcceptAnswerResult>(`/api/v1/answers/${answerId}/accept`),
};
