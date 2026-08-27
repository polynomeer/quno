import { httpClient } from "@/shared/api/http-client";
import type { ClusterDetail } from "./cluster.types";

export const clusterApi = {
  getForQuestion: (questionId: number) => httpClient.get<ClusterDetail>(`/api/v1/questions/${questionId}/cluster`),
  markAsSameProblem: (questionId: number, relatedQuestionId: number) =>
    httpClient.post<{ clusterId: number; memberQuestionIds: number[]; representativeAnswerId: number | null }>(
      `/api/v1/questions/${questionId}/cluster`,
      { relatedQuestionId },
    ),
  designateSuperAnswer: (clusterId: number, answerId: number) =>
    httpClient.post<ClusterDetail>(`/api/v1/clusters/${clusterId}/super-answer`, { answerId }),
};
