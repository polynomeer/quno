export type ReportTargetType = "QUESTION" | "ANSWER";

export type ReportReason = "SPAM" | "DUPLICATE" | "LOW_QUALITY" | "OTHER";

export type ReportStatus = "PENDING" | "DISMISSED" | "ACTIONED";

export interface Report {
  id: number;
  reporterId: number;
  targetType: ReportTargetType;
  targetId: number;
  reason: ReportReason;
  message: string | null;
  status: ReportStatus;
  resolvedBy: number | null;
  resolvedAt: string | null;
  createdAt: string;
}
