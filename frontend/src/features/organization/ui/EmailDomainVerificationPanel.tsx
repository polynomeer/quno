"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { useRequestEmailDomainVerification, useConfirmEmailDomainVerification } from "../hooks/useEmailDomainVerification";
import { Input } from "@/shared/ui/Input";
import { Button } from "@/shared/ui/Button";
import { ApiError } from "@/shared/api/api-error";

/**
 * Business/school email → Verified organization (Phase 23, ADR-0035). Confirming finds-or-creates
 * an organization for the email's domain and auto-joins the caller — there's no per-organization
 * "verify" action, this panel is domain-driven rather than organization-driven.
 */
export function EmailDomainVerificationPanel({ viewerId }: { viewerId: number | undefined }) {
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [code, setCode] = useState("");
  const [sent, setSent] = useState(false);
  const requestVerification = useRequestEmailDomainVerification();
  const confirmVerification = useConfirmEmailDomainVerification(viewerId);

  async function handleRequest() {
    if (!email.trim()) return;
    try {
      await requestVerification.mutateAsync(email.trim());
      setSent(true);
    } catch {
      // error surfaced below via requestVerification.error
    }
  }

  async function handleConfirm() {
    if (!code.trim()) return;
    try {
      const organization = await confirmVerification.mutateAsync(code.trim());
      router.push(`/organizations/${organization.id}`);
    } catch {
      // error surfaced below via confirmVerification.error
    }
  }

  return (
    <div className="space-y-2 rounded-lg border border-border p-4">
      <h2 className="text-sm font-semibold text-text-secondary">업무/학교 이메일로 Verified 조직 가입</h2>
      <p className="text-xs text-text-secondary">gmail.com 같은 공개 웹메일 도메인은 인증할 수 없습니다.</p>

      <div className="flex flex-wrap gap-2">
        <Input
          value={email}
          onChange={(event) => setEmail(event.target.value)}
          placeholder="you@company.com"
          type="email"
          className="max-w-xs"
        />
        <Button variant="secondary" onClick={handleRequest} disabled={requestVerification.isPending || !email.trim()}>
          {requestVerification.isPending ? "발송 중..." : "인증 코드 보내기"}
        </Button>
      </div>
      {requestVerification.isError && (
        <p className="text-sm text-danger">
          {requestVerification.error instanceof ApiError ? requestVerification.error.message : "인증 코드를 보내지 못했습니다."}
        </p>
      )}
      {sent && !requestVerification.isError && (
        <p className="text-sm text-success">인증 코드를 보냈습니다. 메일함을 확인하세요.</p>
      )}

      {sent && (
        <div className="flex flex-wrap gap-2">
          <Input
            value={code}
            onChange={(event) => setCode(event.target.value)}
            placeholder="6자리 코드"
            maxLength={6}
            className="max-w-[10rem]"
          />
          <Button onClick={handleConfirm} disabled={confirmVerification.isPending || !code.trim()}>
            {confirmVerification.isPending ? "확인 중..." : "확인"}
          </Button>
        </div>
      )}
      {confirmVerification.isError && (
        <p className="text-sm text-danger">
          {confirmVerification.error instanceof ApiError ? confirmVerification.error.message : "코드 확인에 실패했습니다."}
        </p>
      )}
    </div>
  );
}
