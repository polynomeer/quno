import { httpClient } from "@/shared/api/http-client";

/** Mirrors backend EngineSuggestionResponse (Phase 78, ADR-0081). */
export type EngineSuggestionKind = "SECRET_REDACTION";
export type EngineSuggestionStatus = "OPEN" | "ACCEPTED" | "DISMISSED" | "SUPERSEDED";
export type EngineField = "title" | "body" | "environment" | "logs";

export interface EngineSuggestion {
  id: number;
  questionId: number;
  targetVersionNumber: number;
  kind: EngineSuggestionKind;
  status: EngineSuggestionStatus;
  summary: string;
  engine: string;
  engineVersion: number;
  inputHash: string;
  changedFields: EngineField[];
  proposedTitle: string | null;
  proposedBody: string | null;
  proposedEnvironment: string | null;
  proposedLogs: string | null;
  createdAt: string;
  resolvedAt: string | null;
}

export const engineApi = {
  /** The question's author only. */
  list: (questionId: number) => httpClient.get<EngineSuggestion[]>(`/api/v1/questions/${questionId}/engine-suggestions`),
  accept: (id: number) => httpClient.post<EngineSuggestion>(`/api/v1/engine-suggestions/${id}/accept`),
  dismiss: (id: number) => httpClient.post<EngineSuggestion>(`/api/v1/engine-suggestions/${id}/dismiss`),
};

export const engineKeys = {
  suggestions: (questionId: number) => ["questions", questionId, "engine-suggestions"] as const,
};
