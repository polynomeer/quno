"use client";

import { useState, type FormEvent } from "react";
import { useRouter } from "next/navigation";
import { useMutation } from "@tanstack/react-query";
import { authApi } from "@/features/auth/api/auth.api";
import { useWithdrawAccount } from "@/features/auth/hooks/useWithdrawAccount";
import { Button } from "@/shared/ui/Button";
import { Input } from "@/shared/ui/Input";
import { ApiError } from "@/shared/api/api-error";

/** 개인정보 다운로드 + 회원 탈퇴(ADR-0046). 둘 다 본인 프로필에서만 렌더링된다(호출부에서
 * isOwnProfile로 가드) — 별도 self-guard는 두지 않는다. */
export function AccountDangerZone() {
  const router = useRouter();
  const [showWithdrawForm, setShowWithdrawForm] = useState(false);
  const [password, setPassword] = useState("");
  const withdraw = useWithdrawAccount();
  const exportData = useMutation({
    mutationFn: () => authApi.exportMyData(),
    onSuccess: (data) => {
      const blob = new Blob([JSON.stringify(data, null, 2)], { type: "application/json" });
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = "quno-my-data.json";
      link.click();
      URL.revokeObjectURL(url);
    },
  });

  async function handleWithdraw(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    try {
      await withdraw.mutateAsync(password);
      router.push("/");
    } catch {
      // error surfaced below via withdraw.error
    }
  }

  return (
    <section className="space-y-3 rounded-md border border-danger/30 p-4">
      <h2 className="text-sm font-semibold text-text-secondary">계정 관리</h2>

      <div className="flex items-center justify-between gap-2">
        <p className="text-xs text-text-secondary">내 프로필과 작성한 질문/답변을 파일로 내려받습니다.</p>
        <Button variant="secondary" onClick={() => exportData.mutate()} disabled={exportData.isPending}>
          {exportData.isPending ? "준비 중..." : "내 데이터 다운로드"}
        </Button>
      </div>

      {!showWithdrawForm ? (
        <Button variant="danger" onClick={() => setShowWithdrawForm(true)}>
          회원 탈퇴
        </Button>
      ) : (
        <form onSubmit={handleWithdraw} className="space-y-2">
          <p className="text-xs text-text-secondary">
            탈퇴하면 로그인할 수 없게 되고 프로필이 익명화됩니다. 이미 작성한 질문/답변은 삭제되지
            않고 &ldquo;탈퇴한 사용자&rdquo;로 표시됩니다. 계속하려면 현재 비밀번호를 입력하세요.
          </p>
          <Input
            type="password"
            placeholder="현재 비밀번호"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            autoComplete="current-password"
            required
          />
          {withdraw.isError && (
            <p className="text-sm text-danger">
              {withdraw.error instanceof ApiError ? withdraw.error.message : "탈퇴에 실패했습니다."}
            </p>
          )}
          <div className="flex gap-2">
            <Button type="submit" variant="danger" disabled={withdraw.isPending}>
              {withdraw.isPending ? "처리 중..." : "탈퇴 확정"}
            </Button>
            <Button type="button" variant="ghost" onClick={() => setShowWithdrawForm(false)}>
              취소
            </Button>
          </div>
        </form>
      )}
    </section>
  );
}
