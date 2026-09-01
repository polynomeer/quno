"use client";

import { Suspense, useEffect, useRef } from "react";
import { useSearchParams } from "next/navigation";
import Link from "next/link";
import { useRequireAuth } from "@/features/auth/hooks/useRequireAuth";
import { useConfirmDirectAskPayment } from "@/features/direct-ask/hooks/useConfirmDirectAskPayment";
import { ApiError } from "@/shared/api/api-error";
import { Skeleton } from "@/shared/ui/Skeleton";

/**
 * Toss's hosted checkout redirects the browser back here — successUrl and failUrl both point at
 * this page (Phase 25, ADR-0037), distinguished by which query params Toss appended
 * (paymentKey/orderId/amount on success, code/message on failure). Confirming the charge here,
 * not client-side trust, is what actually flips the request to PENDING.
 */
function DirectAskCheckoutContent() {
  useRequireAuth();
  const searchParams = useSearchParams();
  const confirmPayment = useConfirmDirectAskPayment();
  // A ref, not state — the guard only needs to survive re-renders, and mutating a ref (rather
  // than calling setState) inside the effect keeps react-hooks/set-state-in-effect happy while
  // still preventing a duplicate confirm call on re-render (e.g. React StrictMode's double effect).
  const attemptedRef = useRef(false);

  const paymentKey = searchParams.get("paymentKey");
  const orderId = searchParams.get("orderId");
  const amount = searchParams.get("amount");
  const failMessage = searchParams.get("message");

  useEffect(() => {
    if (attemptedRef.current || !paymentKey || !orderId || !amount) return;
    attemptedRef.current = true;
    confirmPayment.mutate({ orderId, paymentKey, amount: Number(amount) });
    // confirmPayment is a fresh mutation object every render — only re-run when the redirect's
    // own query params change, not when the mutation object identity changes.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [paymentKey, orderId, amount]);

  if (failMessage) {
    return (
      <div className="space-y-4">
        <h1 className="text-xl font-semibold">결제가 취소되었습니다</h1>
        <p className="text-sm text-danger">{failMessage}</p>
        <Link href="/direct-asks?role=sent" className="text-sm text-brand hover:underline">
          보낸 요청으로 돌아가기
        </Link>
      </div>
    );
  }

  if (!paymentKey || !orderId || !amount) {
    return <p className="text-sm text-text-secondary">잘못된 접근입니다.</p>;
  }

  if (confirmPayment.isIdle || confirmPayment.isPending) {
    return <Skeleton className="h-24 w-full" />;
  }

  if (confirmPayment.isError) {
    return (
      <div className="space-y-4">
        <h1 className="text-xl font-semibold">결제 확인에 실패했습니다</h1>
        <p className="text-sm text-danger">
          {confirmPayment.error instanceof ApiError ? confirmPayment.error.message : "잠시 후 다시 시도해주세요."}
        </p>
        <Link href="/direct-asks?role=sent" className="text-sm text-brand hover:underline">
          보낸 요청으로 돌아가기
        </Link>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      <h1 className="text-xl font-semibold">Direct Ask 요청이 완료되었습니다</h1>
      <p className="text-sm text-text-secondary">결제가 확인되어 상대방에게 요청이 전달되었습니다.</p>
      <Link href="/direct-asks?role=sent" className="text-sm text-brand hover:underline">
        보낸 요청 보기
      </Link>
    </div>
  );
}

export default function DirectAskCheckoutPage() {
  return (
    <Suspense>
      <DirectAskCheckoutContent />
    </Suspense>
  );
}
