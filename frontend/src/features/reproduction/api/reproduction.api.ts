import { httpClient } from "@/shared/api/http-client";
import type { AttachCapsuleInput, ReproductionCapsule, ReproductionPanel, ReproductionRunComparison, ReproductionRunLog, VersionBisect } from "./reproduction.types";

export const reproductionKeys = {
  panel: (questionId: number) => ["questions", questionId, "reproduction"] as const,
  log: (questionId: number, runId: number) => ["questions", questionId, "reproduction", "runs", runId, "log"] as const,
  compare: (questionId: number, runId: number) => ["questions", questionId, "reproduction", "runs", runId, "compare"] as const,
};

export const reproductionApi = {
  panel: (questionId: number) => httpClient.get<ReproductionPanel>(`/api/v1/questions/${questionId}/reproduction`),
  attach: (questionId: number, input: AttachCapsuleInput) =>
    httpClient.put<ReproductionCapsule>(`/api/v1/questions/${questionId}/reproduction`, input),
  requestRerun: (questionId: number, reason: string) =>
    httpClient.post<ReproductionCapsule>(`/api/v1/questions/${questionId}/reproduction/rerun-requests`, { reason }),
  log: (questionId: number, runId: number) =>
    httpClient.get<ReproductionRunLog>(`/api/v1/questions/${questionId}/reproduction/runs/${runId}/log`),
  startBisect: (questionId: number, fromVersion: string, toVersion: string) =>
    httpClient.post<VersionBisect>(`/api/v1/questions/${questionId}/reproduction/bisects`, { fromVersion, toVersion }),
  cancelBisect: (questionId: number, bisectId: number) =>
    httpClient.post<VersionBisect>(`/api/v1/questions/${questionId}/reproduction/bisects/${bisectId}/cancel`),
  decideBisect: (questionId: number, bisectId: number, approve: boolean) =>
    httpClient.post<VersionBisect>(`/api/v1/questions/${questionId}/reproduction/bisects/${bisectId}/decision`, { approve }),
  compare: (questionId: number, runId: number) =>
    httpClient.get<ReproductionRunComparison>(`/api/v1/questions/${questionId}/reproduction/runs/${runId}/compare`),
};
