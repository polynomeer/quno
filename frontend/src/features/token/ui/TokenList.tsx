"use client";

import { useState } from "react";
import { useRevokeToken } from "../hooks/useTokens";
import type { PersonalAccessToken } from "../api/token.types";
import { Button } from "@/shared/ui/Button";
import { FormError } from "@/shared/ui/FormError";
import { relativeTime } from "@/shared/lib/relative-time";

const SCOPE_LABEL = { READ: "읽기", ASK: "질문 작성" } as const;

function stateOf(token: PersonalAccessToken, now: number): "active" | "expired" | "revoked" {
  if (token.revokedAt) return "revoked";
  return new Date(token.expiresAt).getTime() <= now ? "expired" : "active";
}

const STATE_LABEL = { active: "사용 중", expired: "만료됨", revoked: "폐기됨" } as const;

/** The user's tokens, newest first; revoking takes a second click because it can't be undone. */
export function TokenList({ tokens }: { tokens: PersonalAccessToken[] }) {
  const revoke = useRevokeToken();
  const [confirming, setConfirming] = useState<number | null>(null);
  const [now] = useState(() => Date.now());

  if (tokens.length === 0) {
    return <p className="text-sm text-text-secondary">아직 만든 토큰이 없어요.</p>;
  }

  return (
    <div className="space-y-2">
      <FormError error={revoke.error} fallback="토큰을 폐기하지 못했습니다." />
      <ul className="divide-y divide-border overflow-hidden rounded-2xl border border-border">
        {tokens.map((token) => {
          const state = stateOf(token, now);
          return (
            <li key={token.id} className="flex flex-wrap items-center gap-x-4 gap-y-2 px-4 py-3">
              <div className="min-w-0 flex-1 basis-64">
                <p className="flex flex-wrap items-center gap-2 text-sm font-semibold">
                  {token.name}
                  <span
                    className={
                      state === "active"
                        ? "rounded-full bg-success-subtle px-2 py-0.5 text-xs text-success"
                        : "rounded-full bg-surface-subtle px-2 py-0.5 text-xs text-text-secondary"
                    }
                  >
                    {STATE_LABEL[state]}
                  </span>
                </p>
                <p className="mt-0.5 text-xs text-text-secondary">
                  <code className="font-mono">{token.displayPrefix}…</code> · {token.scopes.map((scope) => SCOPE_LABEL[scope]).join(", ")} ·{" "}
                  {token.lastUsedAt ? `마지막 사용 ${relativeTime(token.lastUsedAt)}` : "사용 기록 없음"} ·{" "}
                  {new Date(token.expiresAt).toLocaleDateString("ko-KR")} 만료
                </p>
              </div>
              {state === "active" &&
                (confirming === token.id ? (
                  <div className="flex gap-2">
                    <Button
                      variant="danger"
                      onClick={() => revoke.mutate(token.id, { onSettled: () => setConfirming(null) })}
                      disabled={revoke.isPending}
                    >
                      정말 폐기
                    </Button>
                    <Button variant="ghost" onClick={() => setConfirming(null)}>
                      취소
                    </Button>
                  </div>
                ) : (
                  <Button variant="secondary" onClick={() => setConfirming(token.id)} aria-label={`${token.name} 폐기`}>
                    폐기
                  </Button>
                ))}
            </li>
          );
        })}
      </ul>
    </div>
  );
}
