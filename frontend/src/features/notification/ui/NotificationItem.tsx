import Link from "next/link";
import { relativeTime } from "@/shared/lib/relative-time";
import { cn } from "@/shared/lib/cn";
import { describeNotification } from "../lib/describe-notification";
import {
  notificationCategory,
  type NotificationCategory,
} from "../lib/notification-category";
import type { Notification } from "../api/notification.types";

const TILE: Record<NotificationCategory, { className: string; path: string }> =
  {
    answer: {
      className: "bg-brand-subtle text-brand-on-subtle",
      path: "M21 12a8 8 0 0 1-11.6 7.1L4 20l1-4.6A8 8 0 1 1 21 12z",
    },
    conversation: {
      className: "bg-live-ring text-live",
      path: "M16 12a4 4 0 1 1-8 0 4 4 0 0 1 8 0zM16 12v1.5a2.5 2.5 0 0 0 5 0V12a9 9 0 1 0-3.5 7.1",
    },
    change: {
      className: "bg-surface-subtle text-text-primary",
      path: "M12 8v4l3 2M3 12a9 9 0 1 0 3-6.7L3 8M3 3v5h5",
    },
  };

/** Unread is a dot plus a barely-there tint — design.md #17 warns against a full-row highlight, so
 * the tint stays at 4% and the dot carries the meaning. The icon tile shows the category. */
export function NotificationItem({
  notification,
}: {
  notification: Notification;
}) {
  const { message, href } = describeNotification(notification);

  const tile = TILE[notificationCategory(notification.type)];

  return (
    <li className="text-sm">
      <Link
        href={href}
        className={cn(
          "group flex items-start gap-3.5 px-4 py-3.5 transition-colors hover:bg-canvas/60 sm:px-5",
          !notification.isRead && "bg-brand/[0.04]",
        )}
      >
        <span
          aria-hidden="true"
          className={cn(
            "grid size-10 shrink-0 place-items-center rounded-xl",
            tile.className,
          )}
        >
          <svg
            viewBox="0 0 24 24"
            width="19"
            height="19"
            fill="none"
            stroke="currentColor"
            strokeWidth="2.2"
            strokeLinecap="round"
            strokeLinejoin="round"
          >
            <path d={tile.path} />
          </svg>
        </span>
        <span className="min-w-0 flex-1">
          <span
            className={cn(
              "block group-hover:text-brand",
              notification.isRead
                ? "text-text-body"
                : "font-semibold text-text-primary",
            )}
          >
            {message}
          </span>
          <span className="mt-1 block text-[13px] text-text-secondary">
            {relativeTime(notification.createdAt)}
          </span>
        </span>
        {!notification.isRead && (
          <span
            className="mt-2 size-2.5 shrink-0 rounded-full bg-brand"
            aria-label="읽지 않음"
          />
        )}
      </Link>
    </li>
  );
}
