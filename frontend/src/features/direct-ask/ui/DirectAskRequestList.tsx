"use client";

import Link from "next/link";
import { useRespondDirectAsk } from "../hooks/useRespondDirectAsk";
import { Button } from "@/shared/ui/Button";
import { relativeTime } from "@/shared/lib/relative-time";
import { FormError } from "@/shared/ui/FormError";
import type { DirectAskRequestListItem } from "../api/direct-ask.types";
import { EmptyState } from "@/shared/ui/EmptyState";

const statusLabels: Record<DirectAskRequestListItem["status"], string> = {
  AWAITING_PAYMENT: "결제 대기",
  PENDING: "응답 대기",
  ACCEPTED: "수락됨",
  DECLINED: "거절됨",
};

const statusToneClasses: Record<DirectAskRequestListItem["status"], string> = {
  AWAITING_PAYMENT: "bg-surface-subtle text-text-body",
  PENDING: "bg-warning-subtle text-warning",
  ACCEPTED: "bg-success-subtle text-success",
  DECLINED: "bg-danger-subtle text-danger",
};

function DirectAskRequestRow({ item, role }: { item: DirectAskRequestListItem; role: "sent" | "received" }) {
  const respond = useRespondDirectAsk(item.id);
  const canRespond = role === "received" && item.status === "PENDING";

  return (
    <li className="px-4 py-4 transition-colors hover:bg-canvas/60 sm:px-5">
      <div className="flex flex-wrap items-center gap-2 text-[13px] text-text-secondary">
        <span className={`inline-flex h-[22px] items-center rounded px-2 text-xs font-semibold ${statusToneClasses[item.status]}`}>
          {statusLabels[item.status]}
        </span>
        {role === "received" ? (
          <span>
            요청자{" "}
            <Link href={`/users/${item.requesterId}`} className="font-medium text-text-primary hover:text-brand">
              {item.requesterNickname}
            </Link>
          </span>
        ) : (
          <span>
            대상{" "}
            <Link href={`/users/${item.targetUserId}`} className="font-medium text-text-primary hover:text-brand">
              {item.targetUserNickname}
            </Link>
          </span>
        )}
        <span>· {relativeTime(item.createdAt)}</span>
      </div>
      <Link href={`/questions/${item.questionId}`} className="mt-1.5 block font-semibold leading-snug text-text-primary hover:text-brand">
        {item.questionTitle}
      </Link>
      {item.message && <p className="mt-1 text-sm text-text-body">{item.message}</p>}

      <FormError error={respond.error} fallback="응답을 처리하지 못했습니다." className="mt-2" />

      {canRespond && (
        <div className="mt-3 flex gap-2">
          <Button onClick={() => respond.mutate(true)} disabled={respond.isPending}>
            수락
          </Button>
          <Button variant="secondary" onClick={() => respond.mutate(false)} disabled={respond.isPending}>
            거절 (자동 환불)
          </Button>
        </div>
      )}
    </li>
  );
}

export function DirectAskRequestList({
  items,
  role,
  emptyMessage,
}: {
  items: DirectAskRequestListItem[];
  role: "sent" | "received";
  emptyMessage: string;
}) {
  if (items.length === 0) {
    return <EmptyState message={emptyMessage} />;
  }

  return (
    <ul className="divide-y divide-border overflow-hidden rounded-xl border border-border bg-surface">
      {items.map((item) => (
        <DirectAskRequestRow key={item.id} item={item} role={role} />
      ))}
    </ul>
  );
}
