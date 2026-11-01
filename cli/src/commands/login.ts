import { QunoClient } from "../client.js";
import { saveConfig } from "../config.js";
import type { CommandContext } from "./context.js";

/**
 * Stores a personal access token after checking it against `GET /me`. The token comes from stdin
 * only — as an argument it would sit in shell history (ADR-0080).
 */
export async function loginCommand(
  ctx: CommandContext,
  token: string | null,
  options: { apiUrl?: string; webUrl?: string; env?: NodeJS.ProcessEnv },
): Promise<number> {
  const secret = token?.trim();
  if (!secret) {
    ctx.err("토큰을 표준 입력으로 넘겨 주세요. 예: pbpaste | quno login (토큰은 웹의 /settings/tokens에서 만듭니다)");
    return 2;
  }
  if (!secret.startsWith("quno_pat_")) {
    ctx.err("quno_pat_로 시작하는 개인 액세스 토큰이 아닙니다.");
    return 2;
  }
  const apiUrl = (options.apiUrl ?? ctx.config.apiUrl).replace(/\/+$/, "");
  const me = await new QunoClient(apiUrl, secret).me();
  const path = saveConfig(
    { apiUrl, token: secret, ...(options.webUrl ? { webUrl: options.webUrl.replace(/\/+$/, "") } : {}) },
    options.env,
  );
  ctx.out(`${me.nickname}(으)로 로그인했습니다. 설정: ${path}`);
  return 0;
}

export async function whoamiCommand(ctx: CommandContext): Promise<number> {
  if (!ctx.config.token) {
    ctx.err("저장된 토큰이 없습니다. quno login으로 먼저 로그인해 주세요.");
    return 2;
  }
  const me = await ctx.client.me();
  ctx.out(`${me.nickname} (#${me.id}) · ${ctx.config.apiUrl}`);
  return 0;
}
