/** Mirrors backend ReproductionController (Phase 80, ADR-0087). */
export type ReproductionKind = "SCRIPT" | "GRADLE" | "DOCKERFILE";

/** Exit codes follow `git bisect run`: 0 = not reproduced, 125 = can't tell, 1..127 = reproduced. */
export type ReproductionResult = "REPRODUCED" | "NOT_REPRODUCED" | "BUILD_FAILED" | "INCONCLUSIVE";

export interface ReproductionCapsule {
  id: number;
  repository: string;
  ref: string;
  kind: ReproductionKind;
  command: string;
  createdBy: number;
  createdAt: string;
  rerunRequestedAt: string | null;
  rerunReason: string | null;
  /** The package whose versions the matrix walks (Phase 81, ADR-0088); null = one cell. */
  matrixPackage: string | null;
  matrixVersions: string[];
}

export interface ReproductionRun {
  id: number;
  result: ReproductionResult;
  exitCode: number | null;
  commitSha: string;
  environment: string;
  /** `github-hosted` or `self-hosted`, from the OIDC token. */
  runnerEnvironment: string;
  /** Ran through Quno's reusable workflow rather than a hand-written one. */
  trustedWorkflow: boolean;
  runUrl: string;
  durationMs: number | null;
  finishedAt: string;
  hasLog: boolean;
  /** The matrix cell's version; null outside a matrix. */
  dependencyVersion: string | null;
}

export interface ReproductionPanel {
  capsule: ReproductionCapsule | null;
  /** Newest run per matrix cell (environment × dependencyVersion). */
  latestByCell: ReproductionRun[];
  /** Versions the next run walks; `[null]` without a matrix package. */
  plannedVersions: (string | null)[];
  /** When the scheduled run is next due; null when due now. */
  nextDueAt: string | null;
  recentRuns: ReproductionRun[];
}

export interface ReproductionRunLog {
  runId: number;
  logTail: string | null;
  maskedSecretCount: number;
}

export interface AttachCapsuleInput {
  repository: string;
  ref: string;
  kind: ReproductionKind;
  command: string;
  matrixPackage?: string;
  matrixVersions?: string[];
}
