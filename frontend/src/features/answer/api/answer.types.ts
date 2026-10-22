import type { DiffLine } from "@/features/question/api/question.types";

export interface Answer {
  id: number;
  questionId: number;
  authorId: number;
  /** Set by the list endpoint (ADR-0064); null/absent on create and edit responses. */
  authorNickname?: string | null;
  body: string;
  isAccepted: boolean;
  targetVersionNumber: number;
  isStale: boolean;
  createdAt: string;
  updatedAt: string;
  score: number;
  /** Set when the answer was summarised from a live chat room (Phase 71, ADR-0072). */
  sourceLiveChatRoomId?: number | null;
}

export interface AnswerMutationResult {
  id: number;
  questionId: number;
  versionNumber: number;
}

export interface AnswerVersionSummary {
  versionNumber: number;
  createdBy: number;
  createdByNickname?: string | null;
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
