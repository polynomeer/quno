/** Backend keeps `type` as a plain String (domain/common/OutboxEventTypes), not an enum —
 * these eleven are the only values currently emitted, see docs/architecture/domain-model.md. */
export type NotificationType =
  | "QUESTION_REVISION"
  | "NEW_ANSWER"
  | "ANSWER_ACCEPTED"
  | "REVIEW_REQUESTED"
  | "REVIEW_RE_REQUESTED"
  | "QUESTION_OUTDATED"
  | "NEW_COMMENT"
  | "CONTENT_HIDDEN"
  | "ANSWER_REVISION"
  | "MENTIONED_IN_COMMENT"
  | "TECH_VERSION_IMPACT_DETECTED";

export interface Notification {
  id: number;
  type: NotificationType;
  questionId: number | null;
  answerId: number | null;
  /** Event-specific JSON string — shape differs per type, see describe-notification.ts. */
  payload: string;
  isRead: boolean;
  createdAt: string;
}
