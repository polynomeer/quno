import type { QuestionStatus } from "@/shared/ui/StatusBadge";

export interface WatchedQuestion {
  questionId: number;
  title: string;
  status: QuestionStatus;
}
