import Link from "next/link";
import { relativeTime } from "@/shared/lib/relative-time";
import { cn } from "@/shared/lib/cn";
import { describeNotification } from "../lib/describe-notification";
import type { Notification } from "../api/notification.types";

/** Unread is a small dot, not a full-row highlight — design.md #17 explicitly warns against that. */
export function NotificationItem({ notification }: { notification: Notification }) {
  const { message, href } = describeNotification(notification);

  return (
    <li className={cn("rounded-md border border-border p-3 text-sm", !notification.isRead && "border-brand/40")}>
      <Link href={href} className="flex items-start gap-2 hover:underline">
        {!notification.isRead && (
          <span className="mt-1.5 h-2 w-2 shrink-0 rounded-full bg-brand" aria-label="읽지 않음" />
        )}
        <span className="flex-1">
          <span className="block">{message}</span>
          <span className="mt-1 block text-xs text-text-secondary no-underline">
            {relativeTime(notification.createdAt)}
          </span>
        </span>
      </Link>
    </li>
  );
}
