import { createHash } from "node:crypto";
import { chmodSync, mkdirSync, readFileSync, writeFileSync } from "node:fs";
import { homedir } from "node:os";
import { join } from "node:path";
import type { Probe } from "./collect.js";
import { configKeys } from "./diagnostics.js";
import type { Delta, SnapshotV1 } from "./schema.js";

/**
 * Local state, per project (ADR-0084): the last run that succeeded (the baseline for "what changed
 * since it last worked") and the last failure, frozen. Both stay on this machine, readable only by
 * the user; nothing here is sent unless `quno ask` attaches it after the user has seen it.
 */
export interface SnapshotStore {
  read<T>(name: "baseline" | "last-failure"): T | null;
  write(name: "baseline" | "last-failure", value: unknown): void;
}

export function stateDir(env: NodeJS.ProcessEnv = process.env): string {
  return join(env.XDG_STATE_HOME ?? join(homedir(), ".local", "state"), "quno");
}

export function fileStore(cwd: string, env: NodeJS.ProcessEnv = process.env): SnapshotStore {
  const dir = join(stateDir(env), "projects", createHash("sha256").update(cwd).digest("hex").slice(0, 16));
  return {
    read: <T>(name: string) => {
      try {
        return JSON.parse(readFileSync(join(dir, `${name}.json`), "utf8")) as T;
      } catch {
        return null;
      }
    },
    write: (name, value) => {
      mkdirSync(dir, { recursive: true, mode: 0o700 });
      const path = join(dir, `${name}.json`);
      writeFileSync(path, JSON.stringify(value, null, 2), { mode: 0o600 });
      chmodSync(path, 0o600);
    },
  };
}

export function memoryStore(): SnapshotStore & { data: Record<string, unknown> } {
  const data: Record<string, unknown> = {};
  return {
    data,
    read: <T>(name: string) => (data[name] as T | undefined) ?? null,
    write: (name, value) => {
      data[name] = JSON.parse(JSON.stringify(value));
    },
  };
}

/** A failing run, frozen: the scrubbed snapshot and the scrubbed output tail that becomes the question's logs. */
export interface FrozenFailure {
  frozenAt: string;
  snapshot: SnapshotV1;
  logs: string | null;
}

export interface Baseline {
  capturedAt: string;
  commit: string | null;
  dependencies: Record<string, string>;
  /** Hash of each config file. It stays here; only "this file changed" ever leaves the machine. */
  configFiles: Record<string, string>;
}

const MAX_CHANGED_FILES = 50;

function configHashes(probe: Probe): Record<string, string> {
  return Object.fromEntries(
    configKeys(probe).map(({ file }) => [file, createHash("sha256").update(probe.read(join(probe.cwd, file)) ?? "").digest("hex").slice(0, 16)]),
  );
}

export function baselineOf(probe: Probe, snapshot: SnapshotV1, now = new Date()): Baseline {
  return {
    capturedAt: now.toISOString(),
    commit: snapshot.tier0.git?.commit ?? null,
    dependencies: Object.fromEntries(snapshot.tier0.dependencies.items.map((item) => [item.name, item.version])),
    configFiles: configHashes(probe),
  };
}

/** What changed since [baseline] (Phase 93.4): commits, file names, resolved versions and config files. */
export function deltaSince(probe: Probe, baseline: Baseline, snapshot: SnapshotV1): Delta {
  let commits: number | null = null;
  let changed: string[] = [];
  if (baseline.commit && /^[0-9a-f]{40}$/.test(baseline.commit)) {
    const count = probe.run("git", ["rev-list", "--count", `${baseline.commit}..HEAD`])?.trim();
    if (count && /^\d+$/.test(count)) {
      commits = Number(count);
      // Against the working tree, so uncommitted edits count too.
      changed = (probe.run("git", ["diff", "--name-only", baseline.commit]) ?? "").split("\n").filter(Boolean);
    }
  }

  const now = Object.fromEntries(snapshot.tier0.dependencies.items.map((item) => [item.name, item.version]));
  const names = [...new Set([...Object.keys(baseline.dependencies), ...Object.keys(now)])].sort();
  const dependencies = names.flatMap((name) => {
    const from = baseline.dependencies[name] ?? null;
    const to = now[name] ?? null;
    return from === to ? [] : [{ name, from, to }];
  });

  const currentConfig = configHashes(probe);
  const configFiles = [...new Set([...Object.keys(baseline.configFiles), ...Object.keys(currentConfig)])]
    .filter((file) => baseline.configFiles[file] !== currentConfig[file])
    .sort();

  return {
    since: { capturedAt: baseline.capturedAt, commit: baseline.commit },
    commits,
    changedFiles: changed.slice(0, MAX_CHANGED_FILES),
    changedFileCount: changed.length,
    dependencies,
    configFiles,
  };
}
