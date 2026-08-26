import type { QuestionSummary } from "@/features/question/api/question.types";

export interface Notification {
  id: number;
  type: string;
  questionId: number | null;
  answerId: number | null;
  payload: string;
  isRead: boolean;
  createdAt: string;
}

export interface TagTrend {
  id: number;
  name: string;
  slug: string;
  questionCount: number;
}

export interface TagSpike {
  id: number;
  name: string;
  slug: string;
  recentCount: number;
  baselineAveragePerDay: number;
  spikeRatio: number;
}

export interface DashboardHeadline {
  text: string;
  questionId: number | null;
}

export interface Dashboard {
  popularQuestions: QuestionSummary[];
  wardUpdates: Notification[];
  followingTagsFeed: QuestionSummary[];
  trendingTags: TagTrend[];
  headline: DashboardHeadline | null;
  resolvedToday: QuestionSummary[];
  reopenedKnowledge: QuestionSummary[];
  trendingErrors: TagSpike[];
}

export type FlowCardType = "POPULAR_QUESTION" | "TAG_SPIKE" | "REOPENED_QUESTION" | "CLUSTER_SUPER_ANSWER";

export interface FlowCard {
  type: FlowCardType;
  headline: string;
  questionId: number | null;
  clusterId: number | null;
}
