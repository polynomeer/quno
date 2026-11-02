import { spawn } from "node:child_process";
import { constants } from "node:os";
import { collectSnapshot, type Probe } from "../snapshot/collect.js";
import { composeSnapshot, OmitPathError, summarizeFailure, validateOmit } from "../snapshot/compose.js";
import { LineRing, PROFILES } from "../snapshot/diagnostics.js";
import type { Preset } from "../snapshot/schema.js";
import { baselineOf, type Baseline, type FrozenFailure, type SnapshotStore } from "../snapshot/state.js";
import type { CommandContext } from "./context.js";

export interface RunDeps {
  probe: Probe;
  store: SnapshotStore;
  /** Where the child's output is passed through to — the user's terminal. */
  write: (stream: "stdout" | "stderr", chunk: string) => void;
}

/**
 * `quno run -- <command>` (Phase 93.3/93.5, ADR-0084): runs the command with its output passed
 * through unchanged, keeping only the last lines in memory. On success it records this machine's
 * state as the baseline; on failure it freezes the scrubbed snapshot and output tail locally, for
 * `quno capture --last-failure` to show and `quno ask --last-failure` to attach. Nothing is sent.
 * Exits with the command's own exit code, so it can wrap anything in a script.
 */
export async function runCommand(ctx: CommandContext, argv: string[], options: { preset?: Preset; omit: string[] }, deps: RunDeps): Promise<number> {
  if (argv.length === 0) {
    ctx.err("실행할 명령이 필요합니다. 예: quno run -- ./gradlew test");
    return 2;
  }
  try {
    validateOmit(options.omit);
  } catch (error) {
    if (error instanceof OmitPathError) {
      ctx.err(error.message);
      return 2;
    }
    throw error;
  }

  const ring = new LineRing();
  const startedAtMs = Date.now();
  const child = spawn(argv[0]!, argv.slice(1), { cwd: ctx.cwd, stdio: ["inherit", "pipe", "pipe"] });
  child.stdout.setEncoding("utf8");
  child.stderr.setEncoding("utf8");
  child.stdout.on("data", (chunk: string) => {
    deps.write("stdout", chunk);
    ring.push(chunk);
  });
  child.stderr.on("data", (chunk: string) => {
    deps.write("stderr", chunk);
    ring.push(chunk);
  });
  const result = await new Promise<{ code: number | null; signal: NodeJS.Signals | null; error?: Error }>((resolve) => {
    child.on("error", (error) => resolve({ code: null, signal: null, error }));
    child.on("close", (code, signal) => resolve({ code, signal }));
  });
  if (result.error) {
    ctx.err(`실행하지 못했습니다: ${argv[0]} (${result.error.message})`);
    return 127;
  }

  if (result.code === 0) {
    deps.store.write("baseline", baselineOf(deps.probe, collectSnapshot(deps.probe)));
    return 0;
  }

  const output = ring.text();
  const { snapshot, logs } = composeSnapshot(deps.probe, {
    preset: options.preset,
    omit: options.omit,
    baseline: deps.store.read<Baseline>("baseline"),
    failure: {
      argv,
      exitCode: result.code,
      signal: result.signal,
      durationMs: Date.now() - startedAtMs,
      output,
      totalLines: ring.total,
      keptLines: ring.kept,
      startedAtMs,
    },
  });
  const frozen: FrozenFailure = { frozenAt: new Date().toISOString(), snapshot, logs };
  deps.store.write("last-failure", frozen);

  ctx.err(`[quno] 실패를 이 컴퓨터에만 기록했습니다: ${summarizeFailure(snapshot)}`);
  if (PROFILES[snapshot.tier1?.symptom ?? "UNKNOWN"].tier2Helps && !snapshot.tier2) {
    ctx.err("[quno] 메모리 상태가 원인을 좁히는 데 도움이 될 수 있어요. --preset detailed로 다시 실행하면 메모리·디스크·cgroup 한도(Tier 2)도 담습니다.");
  }
  ctx.err('[quno] 확인: quno capture --last-failure · 질문: quno ask --last-failure --title "..." --body "..."');
  return result.code ?? 128 + (result.signal ? (constants.signals[result.signal] ?? 0) : 0);
}
