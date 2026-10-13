import Link from "next/link";
import { relativeTime } from "@/shared/lib/relative-time";
import { cn } from "@/shared/lib/cn";
import { describeNotification } from "../lib/describe-notification";
import type { Notification } from "../api/notification.types";

/** Unread is a small dot, not a full-row highlight — design.md #17 explicitly warns against that. */
export function NotificationItem({ notification }: { notification: Notification }) {
  const { message, href } = describeNotification(notification);

  return (
    <li className="text-sm transition-colors hover:bg-canvas/60">
      <Link href={href} className="group flex items-start gap-3 px-4 py-4 sm:px-5">
        {!notification.isRead && (
          <span className="mt-1.5 size-2 shrink-0 rounded-full bg-brand" aria-label="읽지 않음" />
        )}
        <span className="min-w-0 flex-1">
          <span
            className={cn(
              "block group-hover:text-brand",
              notification.isRead ? "text-text-body" : "font-medium text-text-primary",
            )}
          >
            {message}
          </span>
          <span className="mt-1 block text-[13px] text-text-secondary">
            {relativeTime(notification.createdAt)}
          </span>
        </span>
      </Link>
    </li>
  );
}
