"use client";

import { useState } from "react";
import { useIssueToken } from "../hooks/useTokens";
import type { IssueTokenInput, TokenScope } from "../api/token.types";
import { Button } from "@/shared/ui/Button";
import { FormError } from "@/shared/ui/FormError";
import { Input } from "@/shared/ui/Input";

const SCOPES: { value: TokenScope; label: string; description: string }[] = [
  { value: "READ", label: "읽기", description: "질문·답변·검색 조회, 에러 시그니처·의존성 미리보기" },
  { value: "ASK", label: "질문 작성", description: "질문 작성과 수정" },
];

const LIFETIMES: IssueTokenInput["lifetimeDays"][] = [30, 90, 365];

/** Issues a token and shows its secret exactly once (Phase 65.1, ADR-0079). */
export function IssueTokenForm() {
  const [name, setName] = useState("");
  const [scopes, setScopes] = useState<Set<TokenScope>>(new Set(["READ"]));
  const [lifetimeDays, setLifetimeDays] = useState<IssueTokenInput["lifetimeDays"]>(90);
  const [secret, setSecret] = useState<string | null>(null);
  const [copied, setCopied] = useState(false);
  const issue = useIssueToken();

  function toggle(scope: TokenScope) {
    const next = new Set(scopes);
    if (next.has(scope)) next.delete(scope);
    else next.add(scope);
    setScopes(next);
  }

  function submit() {
    issue.mutate(
      { name: name.trim(), scopes: [...scopes], lifetimeDays },
      {
        onSuccess: (result) => {
          setSecret(result.secret);
          setCopied(false);
          setName("");
        },
      },
    );
  }

  async function copy() {
    if (!secret) return;
    try {
      await navigator.clipboard.writeText(secret);
      setCopied(true);
    } catch {
      // Clipboard can be unavailable (permissions, insecure origin); the secret stays selectable.
    }
  }

  return (
    <div className="space-y-4">
      {secret && (
        <div role="status" className="space-y-2 rounded-2xl border-[1.5px] border-success-border bg-success-subtle p-4">
          <p className="text-sm font-semibold text-success">새 토큰이 만들어졌어요. 이 화면을 벗어나면 다시 볼 수 없어요.</p>
          <div className="flex flex-wrap items-center gap-2">
            <code className="min-w-0 flex-1 rounded-xl bg-surface px-3 py-2 font-mono text-[13px] break-all text-text-primary">{secret}</code>
            <Button variant="secondary" onClick={copy}>
              {copied ? "복사됨" : "복사"}
            </Button>
          </div>
        </div>
      )}

      <div className="space-y-1">
        <label htmlFor="token-name" className="text-sm font-medium">
          이름
        </label>
        <Input id="token-name" value={name} onChange={(event) => setName(event.target.value)} placeholder="예: 노트북 CLI, Claude Code MCP" maxLength={100} />
      </div>

      <fieldset className="space-y-2">
        <legend className="text-sm font-medium">권한</legend>
        {SCOPES.map((scope) => (
          <label key={scope.value} className="flex cursor-pointer items-start gap-3 text-sm">
            <input
              type="checkbox"
              checked={scopes.has(scope.value)}
              onChange={() => toggle(scope.value)}
              className="mt-0.5 size-[18px] shrink-0 accent-brand"
            />
            <span>
              <span className="font-semibold">{scope.label}</span>
              <span className="block text-xs text-text-secondary">{scope.description}</span>
            </span>
          </label>
        ))}
        <p className="text-xs text-text-secondary">투표·답변·채택·계정 설정·토큰 관리는 어떤 토큰으로도 할 수 없어요.</p>
      </fieldset>

      <label className="flex flex-col gap-1 text-sm font-medium">
        만료
        <select
          value={lifetimeDays}
          onChange={(event) => setLifetimeDays(Number(event.target.value) as IssueTokenInput["lifetimeDays"])}
          className="h-11 w-40 rounded-full border border-border-strong bg-surface px-4 text-sm text-text-primary"
        >
          {LIFETIMES.map((days) => (
            <option key={days} value={days}>
              {days}일
            </option>
          ))}
        </select>
      </label>

      <FormError error={issue.error} fallback="토큰을 만들지 못했습니다." />
      <Button onClick={submit} disabled={!name.trim() || scopes.size === 0 || issue.isPending}>
        {issue.isPending ? "만드는 중..." : "토큰 만들기"}
      </Button>
    </div>
  );
}
