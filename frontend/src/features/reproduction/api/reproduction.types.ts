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
}

export interface ReproductionPanel {
  capsule: ReproductionCapsule | null;
  latestByEnvironment: ReproductionRun[];
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
}
