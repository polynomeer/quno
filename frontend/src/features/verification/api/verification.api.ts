import { httpClient } from "@/shared/api/http-client";

/** Mirrors backend VerificationController (Phase 94, ADR-0089). */
export type VerificationLevel = "L0" | "L1" | "L2" | "L3" | "L4";
export type VerificationFlag = "NOT_REPRODUCING" | "SIGNATURE_MISMATCH" | "STALE" | "DISPUTED";
export type QuestionClassification = "MISUSE" | "NOT_A_BUG";
export type VerificationCheck =
  | "ERROR_SIGNATURE"
  | "ENVIRONMENT_SNAPSHOT"
  | "EXPECTED_ACTUAL"
  | "REPRO_STEPS"
  | "NO_SECRETS"
  | "REPRODUCTION_CAPSULE"
  | "TRUSTED_REPRODUCTION"
  | "SIGNATURE_MATCH";

export interface QuestionVerification {
  level: VerificationLevel;
  flags: VerificationFlag[];
  classification: QuestionClassification | null;
  checks: Record<VerificationCheck, boolean>;
  lastReproducedAt: string | null;
  lastRunUrl: string | null;
}

export const verificationKeys = {
  detail: (questionId: number) => ["questions", questionId, "verification"] as const,
};

export const verificationApi = {
  get: (questionId: number) => httpClient.get<QuestionVerification>(`/api/v1/questions/${questionId}/verification`),
  classify: (questionId: number, classification: QuestionClassification | null) =>
    httpClient.put<QuestionVerification>(`/api/v1/questions/${questionId}/verification/classification`, { classification }),
};
