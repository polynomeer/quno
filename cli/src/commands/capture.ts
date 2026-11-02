import { summarizeSnapshot } from "../snapshot/collect.js";
import type { CommandContext } from "./context.js";

/**
 * `quno capture` (Phase 93.5, ADR-0083): shows exactly what `quno ask` would attach — the
 * preview step of "collect, scrub, preview, consent" happens here, on the user's machine.
 */
export async function captureCommand(ctx: CommandContext, options: { json: boolean }): Promise<number> {
  const snapshot = ctx.captureSnapshot();
  if (!options.json) {
    ctx.err(`환경 스냅샷(Tier 0): ${summarizeSnapshot(snapshot)}`);
    ctx.err("아래 JSON이 질문에 그대로 첨부됩니다. 홈 경로·사용자명·호스트명은 지웠고, 서버에서 비밀값을 한 번 더 가립니다.");
  }
  ctx.out(JSON.stringify(snapshot, null, 2));
  return 0;
}
