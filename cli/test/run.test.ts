import { mkdtempSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { describe, expect, it } from "vitest";
import type { CommandContext } from "../src/commands/context.js";
import { runCommand } from "../src/commands/run.js";
import { memoryStore, type Baseline, type FrozenFailure } from "../src/snapshot/state.js";
import { fakeClient } from "./fake-api.js";
import { fakeProbe } from "./fake-probe.js";

function setup() {
  const cwd = mkdtempSync(join(tmpdir(), "quno-run-"));
  const err: string[] = [];
  const written: string[] = [];
  const ctx: CommandContext = {
    client: fakeClient({}).client,
    config: { apiUrl: "http://api.test", webUrl: "http://web.test", token: null },
    cwd,
    out: () => {},
    err: (line) => err.push(line),
    stdin: null,
    confirm: null,
    captureSnapshot: () => {
      throw new Error("not used");
    },
    lastFailure: () => null,
  };
  const store = memoryStore();
  const deps = { probe: fakeProbe({ cwd }), store, write: (_stream: "stdout" | "stderr", chunk: string) => written.push(chunk) };
  return { ctx, store, deps, err, written };
}

// process.exit() would drop output still buffered on the pipe; the scripts set exitCode instead.
const node = (script: string) => [process.execPath, "-e", script];

describe("quno run", () => {
  it("passes output through, records a baseline on success and sends nothing", async () => {
    const { ctx, store, deps, written, err } = setup();
    expect(await runCommand(ctx, node("console.log('ok')"), { omit: [] }, deps)).toBe(0);
    expect(written.join("")).toBe("ok\n");
    expect((store.data.baseline as Baseline).dependencies).toEqual({});
    expect(store.data["last-failure"]).toBeUndefined();
    expect(err).toEqual([]);
  });

  it("freezes a failure with its exit code, scrubbed tail and symptom, and suggests Tier 2 for memory", async () => {
    const { ctx, store, deps, err } = setup();
    const script = "for (let i = 0; i < 250; i++) console.log('line ' + i); console.error('java.lang.OutOfMemoryError: Java heap space'); console.error('password=Vb1Kc9Wd3Fs6'); process.exitCode = 3";
    expect(await runCommand(ctx, node(script), { omit: [] }, deps)).toBe(3);

    const frozen = store.data["last-failure"] as FrozenFailure;
    expect(frozen.snapshot.tier1).toMatchObject({ symptom: "OUT_OF_MEMORY", command: { exitCode: 3 }, logWindow: { keptLines: 200, totalLines: 252, attachedAs: "logs" } });
    expect(frozen.logs?.split("\n")).toHaveLength(200);
    expect(frozen.logs).toContain("OutOfMemoryError");
    expect(frozen.logs).not.toContain("Vb1Kc9Wd3Fs6");
    expect(err.join("\n")).toContain("메모리 부족");
    expect(err.join("\n")).toContain("--preset detailed");
  });

  it("refuses a bad --omit before running anything, and reports a command that can't start", async () => {
    const { ctx, deps, written } = setup();
    expect(await runCommand(ctx, node("console.log('ran')"), { omit: ["capturedAt"] }, deps)).toBe(2);
    expect(written).toEqual([]);
    expect(await runCommand(ctx, ["quno-no-such-command-xyz"], { omit: [] }, deps)).toBe(127);
  });
});
