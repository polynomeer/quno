import type { QuestionSummary } from "@/features/question/api/question.types";
import type { Answer } from "@/features/answer/api/answer.types";
import type { Tag } from "@/entities/tag/model/tag.types";

export interface UserProfile {
  userId: number;
  nickname: string;
  questions: QuestionSummary[];
  answers: Answer[];
  followedTags: Tag[];
}

export interface UserReputation {
  userId: number;
  questionCount: number;
  answerCount: number;
  acceptedAnswerCount: number;
  superAnswerCount: number;
  score: number;
}
