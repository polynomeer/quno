import type { NotificationType } from "../api/notification.types";

/** Coarse buckets for the notifications filter and icon tiles (web design v2, ADR-0071). */
export type NotificationCategory = "answer" | "conversation" | "change";

const CATEGORY: Record<NotificationType, NotificationCategory> = {
  NEW_ANSWER: "answer",
  ANSWER_ACCEPTED: "answer",
  ANSWER_REVISION: "answer",
  LIVE_CHAT_STARTED: "answer",
  NEW_COMMENT: "conversation",
  MENTIONED_IN_COMMENT: "conversation",
  REVIEW_REQUESTED: "conversation",
  REVIEW_RE_REQUESTED: "conversation",
  DIRECT_ASK_REQUESTED: "conversation",
  DIRECT_ASK_ACCEPTED: "conversation",
  DIRECT_ASK_DECLINED: "conversation",
  QUESTION_REVISION: "change",
  QUESTION_OUTDATED: "change",
  TECH_VERSION_IMPACT_DETECTED: "change",
  CONTENT_HIDDEN: "change",
};

/** Types the backend adds later land in "change" until they're mapped here. */
export function notificationCategory(type: string): NotificationCategory {
  return CATEGORY[type as NotificationType] ?? "change";
}
