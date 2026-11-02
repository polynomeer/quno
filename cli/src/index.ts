#!/usr/bin/env node
import { createReadStream, openSync, readFileSync } from "node:fs";
import { createInterface } from "node:readline/promises";
import { parseArgs } from "node:util";
import { QunoApiError, QunoClient } from "./client.js";
import { loadConfig } from "./config.js";
import { askCommand } from "./commands/ask.js";
import { captureCommand } from "./commands/capture.js";
import type { CommandContext } from "./commands/context.js";
import { errorsCommand } from "./commands/errors.js";
import { loginCommand, whoamiCommand } from "./commands/login.js";
import { runCommand } from "./commands/run.js";
import { searchCommand } from "./commands/search.js";
import { serveStdio } from "./mcp/server.js";
import { realProbe } from "./snapshot/collect.js";
import { composeSnapshot, OmitPathError } from "./snapshot/compose.js";
import type { Preset } from "./snapshot/schema.js";
import { fileStore, type FrozenFailure } from "./snapshot/state.js";

const HELP = `quno — Quno CLI and MCP server

사용법:
  quno login [--api-url URL] [--web-url URL]   표준 입력의 개인 액세스 토큰을 확인하고 저장
  quno whoami                                  토큰 주인 확인
  quno search <검색어> [--limit N] [--json]     질문 검색
  quno errors [--file 로그파일] [--json]         같은 에러를 다룬 질문 찾기(로그는 표준 입력으로도)
  quno run [--preset P] [--omit 필드]... -- <명령> [인자...]
                                               명령을 실행하고, 실패하면 환경 스냅샷과 마지막 출력 200줄을 이 컴퓨터에만 기록
  quno capture [--last-failure] [--preset P] [--omit 필드]... [--json]
                                               질문에 첨부될 환경 스냅샷 미리보기
  quno ask --title T (--body B | --body-file F) [--tag 태그]... [--exclude 패턴]... [--no-stack] [--no-snapshot]
           [--last-failure] [--preset P] [--omit 필드]... [--yes]
                                               질문 작성. 표준 입력은 로그, 빌드 파일은 스택 지문, 이 컴퓨터의 환경은 스냅샷
  quno mcp                                     MCP 서버(stdio)

프리셋(--preset): minimal(환경 식별 정보만) · standard(기본) · detailed(메모리·디스크·cgroup 한도까지, Tier 2)
뺄 필드(--omit): tier0.git, tier0.dependencies, tier1.env, tier1.configKeys, delta처럼 점으로 구분

환경 변수: QUNO_API_URL, QUNO_WEB_URL, QUNO_TOKEN (설정 파일보다 우선)`;

async function readStdin(): Promise<string | null> {
  if (process.stdin.isTTY) return null;
  const chunks: Buffer[] = [];
  for await (const chunk of process.stdin) chunks.push(chunk as Buffer);
  return Buffer.concat(chunks).toString("utf8");
}

/**
 * Asks on the controlling terminal, even when stdin carries piped logs (via /dev/tty). Null when
 * there is no terminal at all — then `ask` insists on --yes.
 */
function terminalConfirm(): CommandContext["confirm"] {
  if (!process.stdout.isTTY) return null;
  let input: NodeJS.ReadableStream = process.stdin;
  if (!process.stdin.isTTY) {
    try {
      input = createReadStream("", { fd: openSync("/dev/tty", "r") });
    } catch {
      return null;
    }
  }
  return async (question) => {
    const rl = createInterface({ input, output: process.stdout });
    try {
      return /^y(es)?$/i.test((await rl.question(question)).trim());
    } finally {
      rl.close();
    }
  };
}

async function main(argv: string[]): Promise<number> {
  const [command, ...rest] = argv;
  const config = loadConfig();
  const client = new QunoClient(config.apiUrl, config.token);

  if (command === "mcp") {
    if (!config.token) console.error("[quno] 토큰이 없어 공개 데이터만 조회합니다. quno login 또는 QUNO_TOKEN을 설정하세요.");
    await serveStdio(client, config.webUrl, { probe: () => realProbe(process.cwd()), store: fileStore(process.cwd()) });
    return -1; // keep running; the transport owns the process from here
  }

  const store = fileStore(process.cwd());
  const optionSpec = {
    preset: { type: "string" },
    omit: { type: "string", multiple: true },
  } as const;
  const presetOf = (value: string | undefined): Preset | undefined | "invalid" =>
    value === undefined ? undefined : value === "minimal" || value === "standard" || value === "detailed" ? value : "invalid";

  if (command === "run") {
    const separator = rest.indexOf("--");
    const { values: runValues } = parseArgs({ args: separator < 0 ? rest : rest.slice(0, separator), options: optionSpec });
    const preset = presetOf(runValues.preset);
    if (preset === "invalid") {
      console.error("--preset은 minimal, standard, detailed 중 하나입니다.");
      return 2;
    }
    if (separator < 0) {
      console.error("명령 앞에 --를 붙여 주세요. 예: quno run -- ./gradlew test");
      return 2;
    }
    const ctx = {
      client,
      config,
      cwd: process.cwd(),
      out: (line: string) => console.log(line),
      err: (line: string) => console.error(line),
      stdin: null,
      confirm: null,
      captureSnapshot: () => composeSnapshot(realProbe(process.cwd())).snapshot,
      lastFailure: () => store.read<FrozenFailure>("last-failure"),
    };
    return runCommand(ctx, rest.slice(separator + 1), { preset, omit: runValues.omit ?? [] }, {
      probe: realProbe(process.cwd()),
      store,
      write: (stream, chunk) => (stream === "stdout" ? process.stdout : process.stderr).write(chunk),
    });
  }

  const { values, positionals } = parseArgs({
    args: rest,
    allowPositionals: true,
    options: {
      limit: { type: "string" },
      json: { type: "boolean", default: false },
      file: { type: "string" },
      title: { type: "string" },
      body: { type: "string" },
      "body-file": { type: "string" },
      tag: { type: "string", multiple: true },
      exclude: { type: "string", multiple: true },
      "no-stack": { type: "boolean", default: false },
      "no-snapshot": { type: "boolean", default: false },
      "last-failure": { type: "boolean", default: false },
      ...optionSpec,
      yes: { type: "boolean", default: false },
      "api-url": { type: "string" },
      "web-url": { type: "string" },
    },
  });

  const preset = presetOf(values.preset);
  if (preset === "invalid") {
    console.error("--preset은 minimal, standard, detailed 중 하나입니다.");
    return 2;
  }
  const omit = values.omit ?? [];

  const needsStdin = command === "login" || command === "errors" || command === "ask";
  const stdin = needsStdin && !(command === "errors" && values.file) ? await readStdin() : null;
  const ctx: CommandContext = {
    client,
    config,
    cwd: process.cwd(),
    out: (line) => console.log(line),
    err: (line) => console.error(line),
    stdin,
    confirm: command === "ask" ? terminalConfirm() : null,
    captureSnapshot: (options) => composeSnapshot(realProbe(process.cwd()), options).snapshot,
    lastFailure: () => store.read<FrozenFailure>("last-failure"),
  };

  switch (command) {
    case "login":
      return loginCommand(ctx, stdin, { apiUrl: values["api-url"], webUrl: values["web-url"] });
    case "whoami":
      return whoamiCommand(ctx);
    case "search": {
      const query = positionals.join(" ").trim();
      if (!query) {
        ctx.err("검색어가 필요합니다. 예: quno search hikari pool");
        return 2;
      }
      return searchCommand(ctx, query, { limit: Number(values.limit ?? 10), json: values.json });
    }
    case "capture":
      return captureCommand(ctx, { json: values.json, lastFailure: values["last-failure"], preset, omit });
    case "errors":
      return errorsCommand(ctx, values.file ? readFileSync(values.file, "utf8") : stdin, { json: values.json });
    case "ask":
      return askCommand(ctx, {
        title: values.title ?? "",
        body: values["body-file"] ? readFileSync(values["body-file"], "utf8") : (values.body ?? ""),
        tags: values.tag ?? [],
        exclude: values.exclude ?? [],
        stack: !values["no-stack"],
        snapshot: !values["no-snapshot"],
        lastFailure: values["last-failure"],
        preset,
        omit,
        yes: values.yes,
      });
    case undefined:
    case "help":
    case "--help":
    case "-h":
      console.log(HELP);
      return 0;
    default:
      console.error(`알 수 없는 명령: ${command}\n\n${HELP}`);
      return 2;
  }
}

main(process.argv.slice(2)).then(
  (code) => {
    if (code >= 0) process.exitCode = code;
  },
  (error: unknown) => {
    if (error instanceof OmitPathError) {
      console.error(error.message);
      process.exitCode = 2;
      return;
    }
    if (error instanceof QunoApiError) {
      const hint = error.status === 401 ? " — 토큰이 없거나 만료·폐기되었습니다. quno login으로 다시 로그인하세요." : "";
      console.error(`오류 (${error.status} ${error.code}): ${error.message}${hint}`);
    } else {
      console.error(error instanceof Error ? error.message : String(error));
    }
    process.exitCode = 1;
  },
);
