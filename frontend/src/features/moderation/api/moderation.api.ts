import { httpClient } from "@/shared/api/http-client";
import type { Report, ReportStatus } from "@/features/report/api/report.types";

export const moderationApi = {
  listReports: (status: ReportStatus) => httpClient.get<Report[]>(`/api/v1/moderation/reports?status=${status}`),
  dismiss: (reportId: number) => httpClient.post<void>(`/api/v1/moderation/reports/${reportId}/dismiss`),
  hide: (reportId: number) => httpClient.post<void>(`/api/v1/moderation/reports/${reportId}/hide`),
};
