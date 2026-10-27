import { httpClient } from "@/shared/api/http-client";
import type { ApplicabilityRange } from "@/features/stack/api/stack.types";
import type { Answer, AnswerMutationResult, AnswerVersionDetail, AnswerVersionDiff, AnswerVersionSummary } from "./answer.types";

export interface AcceptAnswerResult {
  questionId: number;
  answerId: number;
  questionStatus: string;
}

export const answerApi = {
  list: (questionId: number) => httpClient.get<Answer[]>(`/api/v1/questions/${questionId}/answers`),
  /** [liveChatRoomId] cites the live chat the answer was summarised from (ADR-0073); [applicability]
   * says where it holds (ADR-0077). */
  create: (questionId: number, body: string, extra?: { liveChatRoomId?: number; applicability?: ApplicabilityRange[] }) =>
    httpClient.post<Answer>(`/api/v1/questions/${questionId}/answers`, { body, ...extra }),
  accept: (answerId: number) => httpClient.post<AcceptAnswerResult>(`/api/v1/answers/${answerId}/accept`),
  /** Omitting [applicability] keeps the answer's ranges; `[]` clears them (ADR-0077). */
  revise: (answerId: number, body: string, applicability?: ApplicabilityRange[]) =>
    httpClient.post<AnswerMutationResult>(
      `/api/v1/answers/${answerId}/versions`,
      applicability === undefined ? { body } : { body, applicability },
    ),
  listVersions: (answerId: number) => httpClient.get<AnswerVersionSummary[]>(`/api/v1/answers/${answerId}/versions`),
  getVersion: (answerId: number, version: number) =>
    httpClient.get<AnswerVersionDetail>(`/api/v1/answers/${answerId}/versions/${version}`),
  getDiff: (answerId: number, version: number, from?: number) => {
    const query = from !== undefined ? `?from=${from}` : "";
    return httpClient.get<AnswerVersionDiff>(`/api/v1/answers/${answerId}/versions/${version}/diff${query}`);
  },
};
