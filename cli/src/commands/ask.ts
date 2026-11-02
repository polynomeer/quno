import { summarizeSnapshot } from "../snapshot/collect.js";
import { omitFields, summarizeFailure } from "../snapshot/compose.js";
import type { Preset, SnapshotV1 } from "../snapshot/schema.js";
import { dependencyLabel, detectStack } from "../stack.js";
import type { StackDependency } from "../types.js";
import type { CommandContext } from "./context.js";
import { formatSummary } from "./search.js";

export interface AskOptions {
  title: string;
  body: string;
  tags: string[];
  exclude: string[];
  stack: boolean;
  /** Attach this machine's Tier 0 environment snapshot (ADR-0083). */
  snapshot: boolean;
  /** Attach what `quno run` froze: its snapshot, and its output tail as logs unless stdin has logs (ADR-0084). */
  lastFailure?: boolean;
  preset?: Preset;
  omit?: string[];
  yes: boolean;
}

/**
 * `quno ask` (Phase 65.2, ADR-0080): piped stdin becomes the question's logs, the project's build
 * file becomes its stack fingerprint, and nothing is posted before the author has seen what will go
 * out — on a terminal by answering y, otherwise only with --yes.
 */
export async function askCommand(ctx: CommandContext, options: AskOptions): Promise<number> {
  if (!options.title.trim() || !options.body.trim()) {
    ctx.err("--title과 --body(또는 --body-file)가 필요합니다.");
    return 2;
  }
  let snapshot: SnapshotV1 | null = null;
  let failureLogs: string | null = null;
  if (options.lastFailure) {
    const frozen = ctx.lastFailure();
    if (!frozen) {
      ctx.err("기록된 실패가 없습니다. quno run -- <명령>으로 실행하면 실패할 때 기록됩니다.");
      return 2;
    }
    failureLogs = frozen.logs;
    if (options.snapshot) snapshot = omitFields(frozen.snapshot, options.omit ?? []);
  } else if (options.snapshot) {
    snapshot = ctx.captureSnapshot({ preset: options.preset, omit: options.omit ?? [] });
  }
  const logs = ctx.stdin?.trim() ? ctx.stdin : (failureLogs ?? undefined);

  let dependencies: StackDependency[] = [];
  if (options.stack) {
    const detected = await detectStack(ctx.client, ctx.cwd, options.exclude);
    if (detected) {
      dependencies = detected.dependencies;
      const excludedNote = detected.excluded > 0 ? `, ${detected.excluded}개 제외` : "";
      ctx.out(`스택 지문: ${detected.file}에서 ${dependencies.length}개${excludedNote}`);
      dependencies.forEach((dependency) => ctx.out(`  - ${dependencyLabel(dependency)}`));
    } else {
      ctx.out("스택 지문: 현재 디렉터리에서 빌드 파일을 찾지 못했습니다.");
    }
  }

  if (snapshot) {
    const preview = options.lastFailure ? "quno capture --last-failure" : "quno capture";
    ctx.out(`환경 스냅샷: ${summarizeSnapshot(snapshot)} (${preview}로 전체 내용 확인, --no-snapshot으로 제외)`);
    const failure = summarizeFailure(snapshot);
    if (failure) ctx.out(`실패 진단: ${failure}`);
    if (snapshot.omitted?.length) ctx.out(`뺀 필드: ${snapshot.omitted.join(", ")}`);
  }

  if (logs) {
    const preview = await ctx.client.previewErrorSignatures(logs);
    if (preview.questions.length > 0) {
      ctx.out(`같은 에러를 다룬 질문이 이미 ${preview.questions.length}건 있습니다:`);
      preview.questions.slice(0, 3).forEach((question) => ctx.out(formatSummary(question, ctx.config.webUrl)));
    }
  }

  ctx.out(`제목: ${options.title}`);
  const logSource = logs && logs === failureLogs ? " (quno run이 기록한 실패 직전 출력)" : "";
  ctx.out(`로그: ${logs ? `${logs.trimEnd().split("\n").length}줄${logSource}` : "없음"} · 태그: ${options.tags.join(", ") || "없음"}`);

  if (!options.yes) {
    if (!ctx.confirm) {
      ctx.err("표준 입력이 터미널이 아니라 확인을 받을 수 없습니다. 위 내용을 게시하려면 --yes를 붙여 주세요.");
      return 2;
    }
    if (!(await ctx.confirm("이대로 게시할까요? [y/N] "))) {
      ctx.out("게시하지 않았습니다.");
      return 1;
    }
  }

  const created = await ctx.client.createQuestion({
    title: options.title.trim(),
    body: options.body,
    ...(logs ? { logs } : {}),
    ...(options.tags.length > 0 ? { tags: options.tags } : {}),
    ...(dependencies.length > 0 ? { dependencies } : {}),
    ...(snapshot ? { environmentSnapshot: snapshot } : {}),
  });
  ctx.out(`게시했습니다: #${created.id} ${ctx.config.webUrl}/questions/${created.id}`);
  return 0;
}
