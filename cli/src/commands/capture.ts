import { summarizeSnapshot } from "../snapshot/collect.js";
import { omitFields, summarizeFailure } from "../snapshot/compose.js";
import { PROFILES } from "../snapshot/diagnostics.js";
import type { Preset, SnapshotV1 } from "../snapshot/schema.js";
import type { CommandContext } from "./context.js";

export interface CaptureOptions {
  json: boolean;
  lastFailure: boolean;
  preset?: Preset;
  omit: string[];
}

/**
 * `quno capture` (Phase 93.5, ADR-0083/0084): shows exactly what `quno ask` would attach — the
 * preview step of "collect, scrub, preview, consent" happens here, on the user's machine. With
 * --last-failure it shows what `quno run` froze, output tail included.
 */
export async function captureCommand(ctx: CommandContext, options: CaptureOptions): Promise<number> {
  let snapshot: SnapshotV1;
  let logs: string | null = null;
  if (options.lastFailure) {
    const frozen = ctx.lastFailure();
    if (!frozen) {
      ctx.err("기록된 실패가 없습니다. quno run -- <명령>으로 실행하면 실패할 때 기록됩니다.");
      return 2;
    }
    snapshot = omitFields(frozen.snapshot, options.omit);
    logs = frozen.logs;
  } else {
    snapshot = ctx.captureSnapshot({ preset: options.preset, omit: options.omit });
  }

  if (options.json) {
    ctx.out(JSON.stringify(logs === null ? snapshot : { snapshot, logs }, null, 2));
    return 0;
  }
  ctx.err(`환경 스냅샷(Tier ${snapshot.tiers.join("·")}): ${summarizeSnapshot(snapshot)}`);
  const failure = summarizeFailure(snapshot);
  if (failure) ctx.err(`실패: ${failure}`);
  if (snapshot.omitted?.length) ctx.err(`뺀 필드: ${snapshot.omitted.join(", ")}`);
  if (snapshot.tier1 && PROFILES[snapshot.tier1.symptom ?? "UNKNOWN"].tier2Helps && !snapshot.tier2) {
    ctx.err("메모리 상태(Tier 2)는 담지 않았습니다. 필요하면 quno run --preset detailed -- <명령>으로 다시 실행하세요.");
  }
  ctx.err("아래 JSON이 질문에 그대로 첨부됩니다. 홈 경로·사용자명·호스트명과 비밀값으로 보이는 값은 지웠고, 서버에서 한 번 더 가립니다. --omit 필드로 뺄 수 있습니다.");
  ctx.out(JSON.stringify(snapshot, null, 2));
  if (logs !== null) {
    ctx.err(`아래 로그(${logs.split("\n").length}줄)가 질문의 로그 칸에 들어갑니다.`);
    ctx.out(logs);
  }
  return 0;
}
