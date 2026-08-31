import type { QuestionStatus } from "@/shared/ui/StatusBadge";

/** Shared "question summary" shape — backend's QuestionSearchResultResponse, reused across
 * search/dashboard/related/cluster (see docs/architecture/api-design.md QuestionSummaryHydrator). */
export interface QuestionSummary {
  id: number;
  title: string;
  status: QuestionStatus;
  tags: string[];
  score: number;
}

export interface QuestionDetail {
  id: number;
  authorId: number;
  title: string;
  status: QuestionStatus;
  versionNumber: number;
  body: string;
  environment: string | null;
  logs: string | null;
  tags: string[];
  createdAt: string;
  updatedAt: string;
  score: number;
}

export interface QuestionVersionSummary {
  versionNumber: number;
  title: string;
  createdBy: number;
  createdAt: string;
}

export interface QuestionVersionDetail {
  questionId: number;
  versionNumber: number;
  title: string;
  body: string;
  environment: string | null;
  logs: string | null;
  createdBy: number;
  createdAt: string;
}

export type DiffLineType = "EQUAL" | "ADDED" | "REMOVED";

export interface DiffLine {
  type: DiffLineType;
  text: string;
}

export interface QuestionVersionDiff {
  fromVersion: number;
  toVersion: number;
  lines: DiffLine[];
}

export interface CreateQuestionInput {
  title: string;
  body: string;
  environment?: string;
  logs?: string;
  tags: string[];
}

export interface QuestionMutationResult {
  id: number;
  title: string;
  status: QuestionStatus;
  versionNumber: number;
}

/** Composed by the backend from Cluster/Fork/Related lookups it already had (Phase 18,
 * ADR-0030) — a data view, not a graph visualization. `clusterMembers`/`relatedQuestions` are
 * intentionally unused by `ForkPanel`, which only reads the Fork lineage fields — Cluster and
 * Related Questions already have their own dedicated UI elsewhere on the page. */
export interface QuestionGraph {
  questionId: number;
  clusterMembers: QuestionSummary[];
  forkedFrom: QuestionSummary | null;
  forks: QuestionSummary[];
  relatedQuestions: QuestionSummary[];
}
