import { httpClient } from "@/shared/api/http-client";
import type { Report, ReportReason } from "./report.types";

export const reportApi = {
  reportQuestion: (questionId: number, reason: ReportReason, message?: string) =>
    httpClient.post<Report>(`/api/v1/questions/${questionId}/reports`, { reason, message }),
  reportAnswer: (answerId: number, reason: ReportReason, message?: string) =>
    httpClient.post<Report>(`/api/v1/answers/${answerId}/reports`, { reason, message }),
};
