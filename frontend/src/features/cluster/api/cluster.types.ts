import type { QuestionSummary } from "@/features/question/api/question.types";

export interface ClusterDetail {
  clusterId: number;
  members: QuestionSummary[];
  representativeAnswerId: number | null;
}
