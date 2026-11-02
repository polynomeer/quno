/** `quno.snapshot/1` (ADR-0083). The schema only ever adds fields; a breaking change bumps schemaVersion. */
export interface SnapshotV1 {
  schemaVersion: 1;
  source: "COLLECTOR" | "MANUAL";
  collector: { name: string; version: string };
  capturedAt: string;
  tiers: number[];
  tier0: Tier0;
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
