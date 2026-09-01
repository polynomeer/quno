import type { QuestionSummary } from "@/features/question/api/question.types";
import type { Answer } from "@/features/answer/api/answer.types";
import type { Tag } from "@/entities/tag/model/tag.types";
import type { Organization } from "@/entities/organization/model/organization.types";

export interface UserProfile {
  userId: number;
  nickname: string;
  questions: QuestionSummary[];
  answers: Answer[];
  followedTags: Tag[];
  /** Virtual/Community/Verified organizations this user has joined (Phase 22, ADR-0034). */
  organizations: Organization[];
}

export interface UserReputation {
  userId: number;
  questionCount: number;
  answerCount: number;
  acceptedAnswerCount: number;
  superAnswerCount: number;
  score: number;
}
