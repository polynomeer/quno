"use client";

import { useEffect, useRef, useState } from "react";
import { useSession } from "@/features/auth/hooks/useSession";
import { useUserProfile } from "@/entities/user/hooks/useUserProfile";
import { useCreateDirectAsk } from "../hooks/useCreateDirectAsk";
import { useTossPayments } from "../lib/toss";
import { Button } from "@/shared/ui/Button";
import { Textarea } from "@/shared/ui/Textarea";
import { ApiError } from "@/shared/api/api-error";

interface PendingCheckout {
  orderId: string;
  amount: number;
  orderName: string;
  clientKey: string;
}

/**
 * There's no user-search endpoint, so the target is whoever's profile you're already on — you
 * pick which of *your own* questions to attach (backend doesn't require authorship, but asking a
 * question you didn't write makes little sense as a UX). Same self-check pattern as
 * FollowUserButton: hide on your own profile.
 */
export function RequestDirectAskPanel({ targetUserId }: { targetUserId: number }) {
  const { data: me } = useSession();
  const isOwnProfile = Boolean(me && me.id === targetUserId);
  const { data: myProfile } = useUserProfile(me?.id ?? 0, Boolean(me) && !isOwnProfile);
  const [open, setOpen] = useState(false);
  const [questionId, setQuestionId] = useState<number | null>(null);
  const [message, setMessage] = useState("");
  const createDirectAsk = useCreateDirectAsk();
  const [pendingCheckout, setPendingCheckout] = useState<PendingCheckout | null>(null);
  const [launchError, setLaunchError] = useState<string | null>(null);
  const tossPayments = useTossPayments(pendingCheckout?.clientKey);
  // Guards against firing requestPayment twice for the same order (e.g. React StrictMode's
  // double effect) without calling setState inside the effect (react-hooks/set-state-in-effect).
  const launchedOrderIdRef = useRef<string | null>(null);

  // Launches the hosted checkout once the Toss SDK instance is ready — that takes a render or
  // two after createDirectAsk succeeds, since useTossPayments loads the script asynchronously.
  // On success, requestPayment redirects the whole page to Toss and this component unmounts; it
  // only ever resolves/rejects here if that redirect itself couldn't happen.
  useEffect(() => {
    if (!tossPayments || !pendingCheckout || launchedOrderIdRef.current === pendingCheckout.orderId) return;
    launchedOrderIdRef.current = pendingCheckout.orderId;
    const origin = window.location.origin;
    tossPayments
      .requestPayment("카드", {
        amount: pendingCheckout.amount,
        orderId: pendingCheckout.orderId,
        orderName: pendingCheckout.orderName,
        successUrl: `${origin}/direct-asks/checkout`,
        failUrl: `${origin}/direct-asks/checkout`,
      })
      .catch(() => {
        setLaunchError("결제창을 여는 데 실패했습니다. 다시 시도해주세요.");
        setPendingCheckout(null);
      });
  }, [tossPayments, pendingCheckout]);

  if (!me || isOwnProfile) {
    return null;
  }

  const myQuestions = myProfile?.questions ?? [];

  async function handleSubmit() {
    if (!questionId) return;
    try {
      const result = await createDirectAsk.mutateAsync({ questionId, targetUserId, message: message.trim() || undefined });
      setPendingCheckout({
        orderId: result.payment.orderId,
        amount: result.payment.amount,
        orderName: myQuestions.find((q) => q.id === questionId)?.title ?? "Direct Ask",
        clientKey: result.payment.clientKey,
      });
    } catch {
      // error surfaced below via createDirectAsk.error
    }
  }

  if (!open) {
    return (
      <Button variant="secondary" onClick={() => setOpen(true)}>
        Direct Ask 요청
      </Button>
    );
  }

  return (
    <div className="space-y-2 rounded-lg border border-border p-4">
      <h2 className="text-sm font-semibold text-text-secondary">이 사용자에게 Direct Ask 요청</h2>
      {myQuestions.length === 0 ? (
        <p className="text-sm text-text-secondary">먼저 질문을 작성해야 Direct Ask를 요청할 수 있습니다.</p>
      ) : (
        <>
          <select
            value={questionId ?? ""}
            onChange={(event) => setQuestionId(event.target.value ? Number(event.target.value) : null)}
            className="w-full rounded-md border border-border bg-surface px-3 py-2 text-sm text-text-primary focus:outline-none focus:ring-2 focus:ring-brand"
          >
            <option value="">질문 선택...</option>
            {myQuestions.map((question) => (
              <option key={question.id} value={question.id}>
                {question.title}
              </option>
            ))}
          </select>
          <Textarea
            value={message}
            onChange={(event) => setMessage(event.target.value)}
            rows={2}
            placeholder="전달할 메시지 (선택)"
            maxLength={1000}
          />
          {createDirectAsk.isError && (
            <p className="text-sm text-danger">
              {createDirectAsk.error instanceof ApiError ? createDirectAsk.error.message : "요청을 보내지 못했습니다."}
            </p>
          )}
          {launchError && <p className="text-sm text-danger">{launchError}</p>}
          <div className="flex gap-2">
            <Button onClick={handleSubmit} disabled={createDirectAsk.isPending || !questionId || Boolean(pendingCheckout)}>
              {createDirectAsk.isPending || pendingCheckout ? "결제창 여는 중..." : "요청하고 결제하기"}
            </Button>
            <Button variant="ghost" onClick={() => setOpen(false)}>
              취소
            </Button>
          </div>
        </>
      )}
    </div>
  );
}
