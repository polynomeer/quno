import type { ReportStatus } from "@/features/report/api/report.types";

export const moderationKeys = {
  reports: (status: ReportStatus) => ["moderation", "reports", status] as const,
};
