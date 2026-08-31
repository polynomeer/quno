/** Backend keeps these as plain identifiers (domain/badge/BadgeType) — display copy lives in
 * describe-badge.ts, same split as NotificationType/describeNotification. */
export type BadgeType = "FIRST_QUESTION" | "FIRST_ANSWER" | "PROBLEM_SOLVER" | "WELL_RECEIVED" | "TRUSTED_ANSWERER" | "SUPER_ANSWER";

export type BadgeTier = "BRONZE" | "SILVER" | "GOLD";

export interface Badge {
  type: BadgeType;
  tier: BadgeTier;
}
