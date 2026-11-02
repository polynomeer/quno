/** `quno.snapshot/1` (ADR-0083). The schema only ever adds fields; a breaking change bumps schemaVersion. */
export interface SnapshotV1 {
  schemaVersion: 1;
  source: "COLLECTOR" | "MANUAL";
  collector: { name: string; version: string };
  capturedAt: string;
  tiers: number[];
  tier0: Tier0;
  /** Diagnostics of one failing command (Phase 93.3, ADR-0084). */
  tier1?: Tier1;
  /** Opt-in runtime state — only with the `detailed` preset (ADR-0070). */
  tier2?: Tier2;
  /** What changed since the last run that succeeded here (Phase 93.4). */
  delta?: Delta | null;
  /** Dotted paths the user chose to leave out, so a reader knows the gap is deliberate. */
  omitted?: string[];
}

export interface Tier0 {
  os: { name: string; version: string | null; arch: string; kernel: string | null };
  runtimes: { name: string; vendor: string | null; version: string | null }[];
  container: { docker: boolean; wsl: boolean; rosetta: boolean };
  locale: { lang: string | null; encoding: string | null; timezone: string | null };
  tools: { name: string; version: string }[];
  ide: string | null;
  ci: { isCi: boolean; provider: string | null };
  git: { commit: string | null; dirty: boolean | null; remoteHost: string | null } | null;
  dependencies: { resolution: "lockfile" | "none"; source: string | null; items: { ecosystem: "MAVEN" | "NPM"; name: string; version: string }[] };
  jvmArgs: string[];
}

/** Same names as the server's SnapshotSymptom. */
export type Symptom = "OUT_OF_MEMORY" | "DB_TIMEOUT" | "DEPENDENCY_RESOLUTION" | "TEST_FAILURE" | "BUILD_FAILURE";

export interface Tier1 {
  command: { argv: string[]; exitCode: number | null; signal: string | null; durationMs: number | null };
  symptom: Symptom | null;
  /** The output tail itself travels as the question's logs, not inside the document. */
  logWindow: { keptLines: number; totalLines: number; attachedAs: "logs" } | null;
  env?: { name: string; value: string }[];
  configKeys?: { file: string; keys: string[] }[];
  failedTests?: { suite: string; name: string; message: string | null }[];
}

export interface Tier2 {
  memory: { totalMb: number; freeMb: number };
  cpus: number;
  diskFreeMb: number | null;
  cgroupMemoryLimitMb: number | null;
}

export interface Delta {
  since: { capturedAt: string; commit: string | null };
  /** Commits between the baseline and HEAD; null when the baseline commit is gone (rebase). */
  commits: number | null;
  /** File names only — never contents. */
  changedFiles: string[];
  changedFileCount: number;
  dependencies: { name: string; from: string | null; to: string | null }[];
  configFiles: string[];
}

export type Preset = "minimal" | "standard" | "detailed";
