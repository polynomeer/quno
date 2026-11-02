import { collectSnapshot, scrubIdentity, type Probe } from "./collect.js";
import { allowlistedEnv, collectTier2, configKeys, detectSymptom, failedTests, PROFILES, scrubArgv } from "./diagnostics.js";
import { redactDeep, redactSecrets } from "./secrets.js";
import type { Preset, SnapshotV1, Tier1 } from "./schema.js";
import { deltaSince, type Baseline } from "./state.js";

/** One command that failed: what ran, how it ended, and the output tail from memory. */
export interface FailureInput {
  argv: string[];
  exitCode: number | null;
  signal: string | null;
  durationMs: number | null;
  output: string;
  totalLines: number;
  keptLines: number;
  /** Test reports older than this belong to an earlier run. */
  startedAtMs: number;
}

export interface ComposeOptions {
  preset?: Preset;
  omit?: string[];
  failure?: FailureInput | null;
  baseline?: Baseline | null;
  now?: Date;
}

/** `minimal` keeps what identifies the environment and drops what describes the project (ADR-0084). */
const MINIMAL_OMIT = ["tier0.dependencies", "tier0.git", "tier0.jvmArgs", "tier0.tools", "tier0.ide", "tier1.env", "tier1.configKeys", "tier1.failedTests", "delta"];

const OMITTABLE_ROOTS = new Set(["tier0", "tier1", "tier2", "delta"]);

export class OmitPathError extends Error {}

/** Throws [OmitPathError] for a path that can't be left out, before anything runs. */
export function validateOmit(paths: string[]): void {
  for (const path of paths) {
    const keys = path.split(".").filter(Boolean);
    if (keys.length === 0 || !OMITTABLE_ROOTS.has(keys[0]!) || (keys.length === 1 && keys[0] === "tier0")) {
      throw new OmitPathError(`뺄 수 없는 필드입니다: ${path} (tier0.<필드>, tier1, tier1.<필드>, tier2, delta만 뺄 수 있습니다)`);
    }
  }
}

/** Removes dotted [paths] (`tier0.git`, `tier1.env`) and records them under `omitted`. */
export function omitFields(snapshot: SnapshotV1, paths: string[]): SnapshotV1 {
  if (paths.length === 0) return snapshot;
  const copy = JSON.parse(JSON.stringify(snapshot)) as Record<string, unknown>;
  validateOmit(paths);
  const omitted = new Set(snapshot.omitted ?? []);
  for (const path of paths) {
    const keys = path.split(".").filter(Boolean);
    let node: unknown = copy;
    keys.slice(0, -1).forEach((key) => {
      node = node && typeof node === "object" ? (node as Record<string, unknown>)[key] : undefined;
    });
    if (node && typeof node === "object" && keys[keys.length - 1]! in node) {
      delete (node as Record<string, unknown>)[keys[keys.length - 1]!];
      omitted.add(path);
    }
  }
  const result = copy as unknown as SnapshotV1;
  if (!("tier1" in result)) result.tiers = result.tiers.filter((tier) => tier !== 1);
  if (!("tier2" in result)) result.tiers = result.tiers.filter((tier) => tier !== 2);
  result.omitted = [...omitted].sort();
  return result;
}

/**
 * Builds the snapshot `quno run`, `quno capture`, `quno ask` and the MCP tool all share:
 * Tier 0, plus Tier 1 for a failure (sections chosen by the symptom profile), plus Tier 2 only for
 * `detailed`, plus the delta since the last success. Then local scrubbing — secrets and identity in
 * every string — and finally the preset's and the user's omissions. Returns the scrubbed output
 * tail separately: it travels as the question's logs.
 */
export function composeSnapshot(probe: Probe, options: ComposeOptions = {}): { snapshot: SnapshotV1; logs: string | null } {
  const preset = options.preset ?? "standard";
  const base = collectSnapshot(probe, options.now);
  const snapshot: SnapshotV1 = { ...base, tiers: [0] };
  const failure = options.failure ?? null;

  if (failure) {
    const tests = failedTests(probe, failure.startedAtMs);
    const symptom = detectSymptom(failure.output, failure.exitCode, tests.length);
    const profile = PROFILES[symptom ?? "UNKNOWN"];
    const tier1: Tier1 = {
      command: { argv: scrubArgv(failure.argv), exitCode: failure.exitCode, signal: failure.signal, durationMs: failure.durationMs },
      symptom,
      logWindow: failure.output ? { keptLines: failure.keptLines, totalLines: failure.totalLines, attachedAs: "logs" } : null,
    };
    if (profile.sections.includes("env")) tier1.env = allowlistedEnv(probe.env);
    if (profile.sections.includes("configKeys")) tier1.configKeys = configKeys(probe);
    if (profile.sections.includes("failedTests")) tier1.failedTests = tests;
    snapshot.tier1 = tier1;
    snapshot.tiers.push(1);
  }
  if (preset === "detailed") {
    snapshot.tier2 = collectTier2(probe);
    snapshot.tiers.push(2);
  }
  if (options.baseline) snapshot.delta = deltaSince(probe, options.baseline, base);

  const scrubbed = scrubIdentity(redactDeep(snapshot), probe);
  const logs = failure?.output ? scrubIdentity(redactSecrets(failure.output).text, probe) : null;
  const omit = [...(preset === "minimal" ? MINIMAL_OMIT : []), ...(options.omit ?? [])];
  return { snapshot: omitFields(scrubbed, omit), logs };
}

/** For previews: `테스트 실패 · ./gradlew test → 1 · 로그 200/1532줄 · 마지막 성공 이후 커밋 3개`. */
export function summarizeFailure(snapshot: SnapshotV1): string | null {
  const tier1 = snapshot.tier1;
  if (!tier1) return null;
  const delta = snapshot.delta;
  return [
    PROFILES[tier1.symptom ?? "UNKNOWN"].label,
    tier1.command ? `${tier1.command.argv.join(" ")} → ${tier1.command.exitCode ?? tier1.command.signal}` : null,
    tier1.logWindow ? `로그 ${tier1.logWindow.keptLines}/${tier1.logWindow.totalLines}줄` : null,
    tier1.failedTests?.length ? `실패한 테스트 ${tier1.failedTests.length}개` : null,
    delta ? `마지막 성공 이후 커밋 ${delta.commits ?? "?"}개·파일 ${delta.changedFileCount}개·의존성 ${delta.dependencies.length}개 변경` : null,
  ]
    .filter(Boolean)
    .join(" · ");
}
