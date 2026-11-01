"use client";

import { useRequireAuth } from "@/features/auth/hooks/useRequireAuth";
import { useMyTokens } from "@/features/token/hooks/useTokens";
import { IssueTokenForm } from "@/features/token/ui/IssueTokenForm";
import { TokenList } from "@/features/token/ui/TokenList";
import { Skeleton } from "@/shared/ui/Skeleton";

/** Personal access tokens for the CLI and MCP server (Phase 65.1, ADR-0079). */
export default function AccessTokensPage() {
  const { me, isLoading: authLoading } = useRequireAuth();
  const { data: tokens, isLoading } = useMyTokens(Boolean(me));

  if (authLoading || isLoading) {
    return <Skeleton className="h-60 w-full" />;
  }

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-[32px] leading-10 font-bold tracking-tight">액세스 토큰</h1>
        <p className="mt-1 text-sm text-text-secondary">
          CLI나 MCP 서버처럼 브라우저 밖에서 Quno를 쓸 때 필요해요. 토큰은 비밀번호처럼 다뤄 주세요.
        </p>
      </div>

      <section aria-labelledby="issue-token" className="space-y-4 rounded-3xl border border-border bg-surface p-5 sm:p-6">
        <h2 id="issue-token" className="text-[15px] font-bold">
          새 토큰
        </h2>
        <IssueTokenForm />
      </section>

      <section aria-labelledby="my-tokens" className="space-y-3">
        <h2 id="my-tokens" className="text-xl font-bold">
          내 토큰
        </h2>
        <TokenList tokens={tokens ?? []} />
      </section>
    </div>
  );
}
