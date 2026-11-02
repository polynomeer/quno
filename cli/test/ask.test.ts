import { mkdtempSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { describe, expect, it } from "vitest";
import { askCommand, type AskOptions } from "../src/commands/ask.js";
import type { CommandContext } from "../src/commands/context.js";
import { errorsCommand } from "../src/commands/errors.js";
import type { SnapshotV1 } from "../src/snapshot/schema.js";
import { QUESTION_SUMMARY, fakeClient, type Recorded } from "./fake-api.js";

const SNAPSHOT: SnapshotV1 = {
  schemaVersion: 1,
  source: "COLLECTOR",
  collector: { name: "quno-cli", version: "0.2.0" },
  capturedAt: "2026-11-01T00:00:00.000Z",
  tiers: [0],
  tier0: {
    os: { name: "macOS", version: "15.1", arch: "arm64", kernel: "24.1.0" },
    runtimes: [{ name: "java", vendor: "Eclipse Adoptium", version: "21.0.5" }],
    container: { docker: false, wsl: false, rosetta: false },
    locale: { lang: "ko_KR", encoding: "UTF-8", timezone: "Asia/Seoul" },
    tools: [],
    ide: null,
    ci: { isCi: false, provider: null },
    git: null,
    dependencies: { resolution: "none", source: null, items: [] },
    jvmArgs: [],
  },
};

const BOOT = { ecosystem: "GRADLE_PLUGIN", name: "org.springframework.boot", version: "4.0.8" };
const ACME = { ecosystem: "MAVEN", name: "com.acme:billing-common", version: "0.14.0" };

function setup(stdin: string | null, confirm: CommandContext["confirm"]) {
  const dir = mkdtempSync(join(tmpdir(), "quno-ask-"));
  writeFileSync(join(dir, "build.gradle.kts"), 'plugins { id("org.springframework.boot") version "4.0.8" }');
  const { client, calls } = fakeClient({
    "POST /api/v1/stack-fingerprints/parse": () => ({ json: { format: "GRADLE", dependencies: [BOOT, ACME] } }),
    "POST /api/v1/error-signatures/preview": () => ({ json: { signatures: [{ exceptionClass: "java.sql.SQLTransientConnectionException" }], questions: [QUESTION_SUMMARY] } }),
    "POST /api/v1/questions": () => ({ status: 201, json: { id: 99 } }),
  });
  const out: string[] = [];
  const err: string[] = [];
  const ctx: CommandContext = {
    client,
    config: { apiUrl: "http://api.test", webUrl: "http://web.test", token: "quno_pat_test" },
    cwd: dir,
    out: (line) => out.push(line),
    err: (line) => err.push(line),
    stdin,
    confirm,
    captureSnapshot: () => SNAPSHOT,
    lastFailure: () => null,
  };
  return { ctx, calls, out, err };
}

const OPTIONS: AskOptions = { title: "Pool exhausted", body: "Connections never return.", tags: [], exclude: ["com.acme:*"], stack: true, snapshot: true, yes: false };
const posts = (calls: Recorded[]) => calls.filter((c) => c.method === "POST" && c.path === "/api/v1/questions");

describe("quno ask", () => {
  it("shows what will go out, then posts logs and the kept stack after a yes", async () => {
    const { ctx, calls, out } = setup("java.sql.SQLTransientConnectionException: HikariPool-1\n\tat x", async () => true);

    expect(await askCommand(ctx, OPTIONS)).toBe(0);

    expect(out).toContain("스택 지문: build.gradle.kts에서 1개, 1개 제외");
    expect(out.some((line) => line.startsWith("환경 스냅샷: macOS 15.1 arm64 · Eclipse Adoptium 21.0.5 · 잠금 파일 없음"))).toBe(true);
    expect(out.some((line) => line.includes("같은 에러를 다룬 질문이 이미 1건"))).toBe(true);
    expect(posts(calls)[0]).toMatchObject({
      body: {
        title: "Pool exhausted",
        body: "Connections never return.",
        logs: "java.sql.SQLTransientConnectionException: HikariPool-1\n\tat x",
        dependencies: [BOOT],
        environmentSnapshot: SNAPSHOT,
      },
    });
    expect(out.at(-1)).toBe("게시했습니다: #99 http://web.test/questions/99");
  });

  it("posts nothing when the author says no", async () => {
    const { ctx, calls } = setup(null, async () => false);
    expect(await askCommand(ctx, OPTIONS)).toBe(1);
    expect(posts(calls)).toHaveLength(0);
  });

  it("refuses to post without a terminal unless --yes is given", async () => {
    const piped = setup("log", null);
    expect(await askCommand(piped.ctx, OPTIONS)).toBe(2);
    expect(posts(piped.calls)).toHaveLength(0);

    const confirmed = setup("log", null);
    expect(await askCommand(confirmed.ctx, { ...OPTIONS, yes: true })).toBe(0);
    expect(posts(confirmed.calls)).toHaveLength(1);
  });

  it("leaves the stack and the snapshot out with --no-stack --no-snapshot, and needs a title and body", async () => {
    const { ctx, calls } = setup(null, async () => true);
    await askCommand(ctx, { ...OPTIONS, stack: false, snapshot: false });
    expect(calls.some((c) => c.path === "/api/v1/stack-fingerprints/parse")).toBe(false);
    expect(posts(calls)[0]?.body).not.toHaveProperty("dependencies");
    expect(posts(calls)[0]?.body).not.toHaveProperty("environmentSnapshot");

    expect(await askCommand(ctx, { ...OPTIONS, body: " " })).toBe(2);
  });
});

describe("quno errors", () => {
  it("lists questions with the same error", async () => {
    const { ctx, out } = setup(null, null);
    expect(await errorsCommand(ctx, "java.sql.SQLTransientConnectionException: x", { json: false })).toBe(0);
    expect(out[0]).toBe("에러 시그니처: java.sql.SQLTransientConnectionException");
    expect(out[2]).toContain("#11 [OPEN] HikariPool connection is not available (답변 2 ✓채택)");
  });

  it("needs some input", async () => {
    const { ctx } = setup(null, null);
    expect(await errorsCommand(ctx, "  ", { json: false })).toBe(2);
  });
});
