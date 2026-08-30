"use client";

import { useRequireAuth } from "@/features/auth/hooks/useRequireAuth";
import { useNotifications } from "@/features/notification/hooks/useNotifications";
import { useMarkAllNotificationsRead } from "@/features/notification/hooks/useMarkAllNotificationsRead";
import { NotificationItem } from "@/features/notification/ui/NotificationItem";
import { Button } from "@/shared/ui/Button";
import { Skeleton } from "@/shared/ui/Skeleton";

export default function NotificationsPage() {
  const { isLoading: authLoading } = useRequireAuth();
  const { data: notifications, isLoading } = useNotifications(true);
  const markAllRead = useMarkAllNotificationsRead();

  if (authLoading || isLoading) {
    return <Skeleton className="h-40 w-full" />;
  }

  const sorted = [...(notifications ?? [])].sort(
    (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime(),
  );
  const unreadCount = sorted.filter((n) => !n.isRead).length;

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-semibold">Notifications</h1>
        {unreadCount > 0 && (
          <Button variant="secondary" onClick={() => markAllRead.mutate()} disabled={markAllRead.isPending}>
            {markAllRead.isPending ? "처리 중..." : `Mark all as read (${unreadCount})`}
          </Button>
        )}
      </div>

      {sorted.length === 0 ? (
        <p className="text-sm text-text-secondary">알림이 없습니다.</p>
      ) : (
        <ul className="space-y-2">
          {sorted.map((notification) => (
            <NotificationItem key={notification.id} notification={notification} />
          ))}
        </ul>
      )}
    </div>
  );
}
