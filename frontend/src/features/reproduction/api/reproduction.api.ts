import { httpClient } from "@/shared/api/http-client";
import type { AttachCapsuleInput, ReproductionCapsule, ReproductionPanel, ReproductionRunLog } from "./reproduction.types";

export const reproductionKeys = {
  panel: (questionId: number) => ["questions", questionId, "reproduction"] as const,
  log: (questionId: number, runId: number) => ["questions", questionId, "reproduction", "runs", runId, "log"] as const,
};

export const reproductionApi = {
  panel: (questionId: number) => httpClient.get<ReproductionPanel>(`/api/v1/questions/${questionId}/reproduction`),
  attach: (questionId: number, input: AttachCapsuleInput) =>
    httpClient.put<ReproductionCapsule>(`/api/v1/questions/${questionId}/reproduction`, input),
  requestRerun: (questionId: number, reason: string) =>
    httpClient.post<ReproductionCapsule>(`/api/v1/questions/${questionId}/reproduction/rerun-requests`, { reason }),
  log: (questionId: number, runId: number) =>
    httpClient.get<ReproductionRunLog>(`/api/v1/questions/${questionId}/reproduction/runs/${runId}/log`),
};
