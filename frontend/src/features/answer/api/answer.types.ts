import type { DiffLine } from "@/features/question/api/question.types";

export interface Answer {
  id: number;
  questionId: number;
  authorId: number;
  body: string;
  isAccepted: boolean;
  targetVersionNumber: number;
  isStale: boolean;
  createdAt: string;
  updatedAt: string;
  score: number;
}

export interface AnswerMutationResult {
  id: number;
  questionId: number;
  versionNumber: number;
}

export interface AnswerVersionSummary {
  versionNumber: number;
  createdBy: number;
  createdAt: string;
}

export interface AnswerVersionDetail {
  answerId: number;
  versionNumber: number;
  body: string;
  createdBy: number;
  createdAt: string;
}

/** DiffLine/DiffLineType are reused as-is from Question — the backend's TextDiffer is a generic
 * two-string differ, not Question-specific (see ADR-0029), so the frontend shape matches too. */
export interface AnswerVersionDiff {
  fromVersion: number;
  toVersion: number;
  lines: DiffLine[];
}
