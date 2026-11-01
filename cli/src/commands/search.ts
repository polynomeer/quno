import type { QuestionSummary } from "../types.js";
import type { CommandContext } from "./context.js";

export function formatSummary(question: QuestionSummary, webUrl: string): string {
  const accepted = question.hasAcceptedAnswer ? " ✓채택" : "";
  return `#${question.id} [${question.status}] ${question.title} (답변 ${question.answerCount}${accepted})\n    ${webUrl}/questions/${question.id}`;
}

export async function searchCommand(ctx: CommandContext, query: string, options: { limit: number; json: boolean }): Promise<number> {
  const results = await ctx.client.search(query, options.limit);
  if (options.json) {
    ctx.out(JSON.stringify(results, null, 2));
    return 0;
  }
  if (results.length === 0) {
    ctx.out("검색 결과가 없습니다.");
    return 0;
  }
  results.forEach((question) => ctx.out(formatSummary(question, ctx.config.webUrl)));
  return 0;
}
