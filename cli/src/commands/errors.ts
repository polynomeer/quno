import type { CommandContext } from "./context.js";
import { formatSummary } from "./search.js";

/** `cat error.log | quno errors` — the same-error lookup the ask form shows (Phase 64, ADR-0075). */
export async function errorsCommand(ctx: CommandContext, text: string | null, options: { json: boolean }): Promise<number> {
  if (!text?.trim()) {
    ctx.err("에러 로그를 표준 입력으로 넘기거나 --file로 지정해 주세요. 예: cat error.log | quno errors");
    return 2;
  }
  const preview = await ctx.client.previewErrorSignatures(text);
  if (options.json) {
    ctx.out(JSON.stringify(preview, null, 2));
    return 0;
  }
  if (preview.signatures.length === 0) {
    ctx.out("예외를 찾지 못했습니다. 키워드로 찾으려면 quno search를 써 주세요.");
    return 0;
  }
  ctx.out(`에러 시그니처: ${preview.signatures.map((s) => s.exceptionClass).join(", ")}`);
  if (preview.questions.length === 0) {
    ctx.out("같은 에러를 다룬 질문이 아직 없습니다. quno ask로 처음 물어볼 수 있어요.");
    return 0;
  }
  ctx.out(`같은 에러 ${preview.questions.length}건:`);
  preview.questions.forEach((question) => ctx.out(formatSummary(question, ctx.config.webUrl)));
  return 0;
}
