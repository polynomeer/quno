import type { QuestionStatus } from "@/shared/ui/StatusBadge";
import type { StackDependency } from "@/features/stack/api/stack.types";

/** Shared "question summary" shape — backend's QuestionSearchResultResponse, reused across
 * search/dashboard/related/cluster (see docs/architecture/api-design.md QuestionSummaryHydrator). */
export interface QuestionSummary {
  id: number;
  title: string;
  status: QuestionStatus;
  tags: string[];
  score: number;
  /** Movement signals for the Living Question Card (ADR-0062). */
  answerCount: number;
  hasAcceptedAnswer: boolean;
  versionNumber: number;
  viewCount: number;
  createdAt: string;
  updatedAt: string;
}

/** "질문의 생애" panel — `GET /questions/{id}/timeline`, newest first (ADR-0062). Acceptance is the
 * `accepted` flag on an ANSWER_POSTED event; the backend doesn't record when it happened. */
export type QuestionTimelineEventType =
  | "QUESTION_CREATED"
  | "QUESTION_REVISED"
  | "ANSWER_POSTED"
  | "REVIEW_REQUESTED"
  | "REVIEW_ADDRESSED";

export interface QuestionTimelineEvent {
  type: QuestionTimelineEventType;
  occurredAt: string;
  actorId: number | null;
  actorNickname?: string | null;
  versionNumber: number | null;
  answerId: number | null;
  accepted: boolean;
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
  /** Distinct viewers, deduplicated server-side per 30 minutes (ADR-0063). */
  viewCount: number;
  /** Stack fingerprint of the latest version (ADR-0076); absent from older responses. */
  dependencies?: StackDependency[];
  /** Secret-looking values masked for this reader by QunoBot (ADR-0081); 0 for the author. */
  maskedSecretCount?: number;
  /** The latest environment snapshot (Phase 93, ADR-0083), or null. */
  environmentSnapshot?: EnvironmentSnapshot | null;
}

/** Mirrors backend EnvironmentSnapshotResponse; `document` is the scrubbed `quno.snapshot/1`. */
export interface EnvironmentSnapshot {
  versionNumber: number;
  schemaVersion: number;
  source: "COLLECTOR" | "MANUAL";
  collector: string | null;
  fingerprint: string;
  serverRedactions: number;
  /** From `tier1.symptom` (Phase 93.3, ADR-0084); null for Tier 0-only snapshots. */
  symptom?: SnapshotSymptom | null;
  createdAt: string;
  document: {
    capturedAt?: string;
    tiers?: number[];
    tier0?: {
      os?: { name?: string; version?: string | null; arch?: string };
      runtimes?: { name: string; vendor: string | null; version: string | null }[];
      container?: { docker?: boolean; wsl?: boolean; rosetta?: boolean };
      locale?: { lang?: string | null; encoding?: string | null; timezone?: string | null };
      tools?: { name: string; version: string }[];
      ide?: string | null;
      ci?: { isCi?: boolean; provider?: string | null };
      git?: { commit?: string | null; dirty?: boolean | null; remoteHost?: string | null } | null;
      dependencies?: { resolution?: string; source?: string | null; items?: { ecosystem: string; name: string; version: string }[] };
      jvmArgs?: string[];
    };
    tier1?: {
      command?: { argv?: string[]; exitCode?: number | null; signal?: string | null; durationMs?: number | null };
      symptom?: SnapshotSymptom | null;
      logWindow?: { keptLines: number; totalLines: number; attachedAs?: string } | null;
      env?: { name: string; value: string }[];
      configKeys?: { file: string; keys: string[] }[];
      failedTests?: { suite: string; name: string; message: string | null }[];
    };
    tier2?: {
      memory?: { totalMb: number; freeMb: number };
      cpus?: number;
      diskFreeMb?: number | null;
      cgroupMemoryLimitMb?: number | null;
    };
    delta?: {
      since?: { capturedAt?: string; commit?: string | null };
      commits?: number | null;
      changedFiles?: string[];
      changedFileCount?: number;
      dependencies?: { name: string; from: string | null; to: string | null }[];
      configFiles?: string[];
    } | null;
    omitted?: string[];
  };
}

export type SnapshotSymptom = "OUT_OF_MEMORY" | "DB_TIMEOUT" | "DEPENDENCY_RESOLUTION" | "TEST_FAILURE" | "BUILD_FAILURE";

export interface QuestionVersionSummary {
  versionNumber: number;
  title: string;
  createdBy: number;
  createdByNickname?: string | null;
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
  /** Only the entries the author kept from the parse preview (ADR-0076). */
  dependencies?: StackDependency[];
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
