import { summarizeSnapshot } from "../snapshot/collect.js";
import type { SnapshotV1 } from "../snapshot/schema.js";
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
  const logs = ctx.stdin?.trim() ? ctx.stdin : undefined;

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

  let snapshot: SnapshotV1 | null = null;
  if (options.snapshot) {
    snapshot = ctx.captureSnapshot();
    ctx.out(`환경 스냅샷: ${summarizeSnapshot(snapshot)} (quno capture로 전체 내용 확인, --no-snapshot으로 제외)`);
  }

  if (logs) {
    const preview = await ctx.client.previewErrorSignatures(logs);
    if (preview.questions.length > 0) {
      ctx.out(`같은 에러를 다룬 질문이 이미 ${preview.questions.length}건 있습니다:`);
      preview.questions.slice(0, 3).forEach((question) => ctx.out(formatSummary(question, ctx.config.webUrl)));
    }
  }

  ctx.out(`제목: ${options.title}`);
  ctx.out(`로그: ${logs ? `${logs.trimEnd().split("\n").length}줄` : "없음"} · 태그: ${options.tags.join(", ") || "없음"}`);

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
