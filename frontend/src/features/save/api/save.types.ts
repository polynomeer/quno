import type { QuestionStatus } from "@/shared/ui/StatusBadge";

export interface SavedQuestion {
  questionId: number;
  title: string;
  status: QuestionStatus;
}
