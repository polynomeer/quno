"use client";

import { useState } from "react";
import { useRequireAuth } from "@/features/auth/hooks/useRequireAuth";
import { useNotifications } from "@/features/notification/hooks/useNotifications";
import { useMarkAllNotificationsRead } from "@/features/notification/hooks/useMarkAllNotificationsRead";
import { NotificationItem } from "@/features/notification/ui/NotificationItem";
import {
  notificationCategory,
  type NotificationCategory,
} from "@/features/notification/lib/notification-category";
import type { Notification } from "@/features/notification/api/notification.types";
import { useDashboard } from "@/features/dashboard/hooks/useDashboard";
import { SideNav } from "@/widgets/side-nav/SideNav";
import { Button } from "@/shared/ui/Button";
import { EmptyState } from "@/shared/ui/EmptyState";
import {
  SegmentedControl,
  type SegmentOption,
} from "@/shared/ui/SegmentedControl";
import { Skeleton } from "@/shared/ui/Skeleton";

type Filter = "all" | NotificationCategory;

const FILTER_LABELS: Record<Filter, string> = {
  all: "전체",
  answer: "답변",
  conversation: "댓글·요청",
  change: "질문 변화",
};

/** Today vs earlier, by the viewer's local calendar day. */
function isToday(iso: string, now: Date): boolean {
  const date = new Date(iso);
  return (
    date.getFullYear() === now.getFullYear() &&
    date.getMonth() === now.getMonth() &&
    date.getDate() === now.getDate()
  );
}

function Group({ label, items }: { label: string; items: Notification[] }) {
  if (items.length === 0) return null;
  return (
    <section aria-label={label} className="space-y-2">
      <h2 className="px-1.5 pt-1 text-[13px] font-bold text-text-secondary">
        {label}
      </h2>
      <ul className="divide-y divide-border overflow-hidden rounded-[20px] border border-border bg-surface">
        {items.map((notification) => (
          <NotificationItem key={notification.id} notification={notification} />
        ))}
      </ul>
    </section>
  );
}

export default function NotificationsPage() {
  const { me, isLoading: authLoading } = useRequireAuth();
  const { data: notifications, isLoading } = useNotifications(true);
  const { data: dashboard } = useDashboard(Boolean(me));
  const markAllRead = useMarkAllNotificationsRead();
  const [filter, setFilter] = useState<Filter>("all");

  if (authLoading || isLoading) {
    return <Skeleton className="h-40 w-full" />;
  }

  const sorted = [...(notifications ?? [])].sort(
    (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime(),
  );
  const unreadCount = sorted.filter((n) => !n.isRead).length;
  const counts: Record<Filter, number> = {
    all: sorted.length,
    answer: 0,
    conversation: 0,
    change: 0,
  };
  for (const notification of sorted)
    counts[notificationCategory(notification.type)] += 1;
  const options: SegmentOption<Filter>[] = (
    Object.keys(FILTER_LABELS) as Filter[]
  ).map((value) => ({
    value,
    label: FILTER_LABELS[value],
    count: counts[value],
  }));
  const shown =
    filter === "all"
      ? sorted
      : sorted.filter((n) => notificationCategory(n.type) === filter);
  const now = new Date();

  return (
    <div className="grid grid-cols-1 gap-8 xl:grid-cols-[200px_minmax(0,1fr)]">
      <div className="hidden xl:block">
        <div className="sticky top-28">
          <SideNav
            wardChangeCount={
              dashboard?.wardUpdates.filter((update) => !update.isRead)
                .length ?? 0
            }
            userId={me?.id}
            unreadNotificationCount={unreadCount}
          />
        </div>
      </div>
      <div className="min-w-0 max-w-[860px] space-y-5">
        <div className="flex flex-wrap items-end justify-between gap-x-6 gap-y-3">
          <div>
            <h1 className="text-[32px] leading-10 font-bold tracking-tight">
              알림
            </h1>
            <p className="mt-0.5 text-sm text-text-secondary">
              읽지 않은 알림 {unreadCount}개
            </p>
          </div>
          <div className="flex flex-wrap items-center gap-2.5">
            {sorted.length > 0 && (
              <SegmentedControl
                options={options}
                value={filter}
                onChange={setFilter}
                label="알림 종류"
              />
            )}
            {unreadCount > 0 && (
              <Button
                variant="secondary"
                onClick={() => markAllRead.mutate()}
                disabled={markAllRead.isPending}
              >
                {markAllRead.isPending ? "처리 중..." : "모두 읽음"}
              </Button>
            )}
          </div>
        </div>

        {shown.length === 0 ? (
          <EmptyState message="알림이 없습니다." />
        ) : (
          <>
            <Group
              label="오늘"
              items={shown.filter((n) => isToday(n.createdAt, now))}
            />
            <Group
              label="이전"
              items={shown.filter((n) => !isToday(n.createdAt, now))}
            />
          </>
        )}
      </div>
    </div>
  );
}
