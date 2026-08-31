"use client";

import { useRequireAuth } from "@/features/auth/hooks/useRequireAuth";
import { useReports } from "@/features/moderation/hooks/useReports";
import { ReportQueueItem } from "@/features/moderation/ui/ReportQueueItem";
import { Skeleton } from "@/shared/ui/Skeleton";
import { ApiError } from "@/shared/api/api-error";

/**
 * No nav link points here on purpose (ADR-0028) — there's no role management UI either, so a
 * moderator is expected to know this URL directly, same spirit as "promote via DB, not an API".
 * The page itself just leans on the backend's own 403 to gate access.
 */
export default function ModerationPage() {
  const { isLoading: authLoading } = useRequireAuth();
  const { data: reports, isLoading, isError, error } = useReports("PENDING");

  if (authLoading || isLoading) {
    return <Skeleton className="h-40 w-full" />;
  }

  if (isError) {
    const message = error instanceof ApiError && error.status === 403 ? "모더레이터만 접근할 수 있습니다." : "신고 목록을 불러오지 못했습니다.";
    return <p className="text-sm text-danger">{message}</p>;
  }

  return (
    <div className="space-y-4">
      <h1 className="text-xl font-semibold">Moderation Queue</h1>
      {reports && reports.length === 0 ? (
        <p className="text-sm text-text-secondary">대기 중인 신고가 없습니다.</p>
      ) : (
        <ul className="space-y-3">{reports?.map((report) => <ReportQueueItem key={report.id} report={report} />)}</ul>
      )}
    </div>
  );
}
