"use client";

import { useQuery } from "@tanstack/react-query";
import { useSession } from "@/features/auth/hooks/useSession";
import { httpClient } from "@/shared/api/http-client";
import { StatusBadge, type QuestionStatus } from "@/shared/ui/StatusBadge";
import { TagChip } from "@/shared/ui/TagChip";
import { Skeleton } from "@/shared/ui/Skeleton";

interface QuestionSummary {
  id: number;
  title: string;
  status: QuestionStatus;
  tags: string[];
}

interface DashboardResponse {
  popularQuestions: QuestionSummary[];
}

/**
 * Temporary smoke-test home page proving the full stack (Next.js → CORS → JWT → Postgres)
 * works end to end. Frontend Phase 1 (see PLAN.md) replaces this with the real Home/Feed
 * design from docs/frontend/design.md #9.
 */
function useDashboard(enabled: boolean) {
  return useQuery({
    queryKey: ["dashboard"],
    queryFn: () => httpClient.get<DashboardResponse>("/api/v1/dashboard"),
    enabled,
  });
}

export default function HomePage() {
  const { data: me, isLoading: sessionLoading } = useSession();
  const { data: dashboard, isLoading: dashboardLoading } = useDashboard(Boolean(me));

  if (sessionLoading) {
    return <Skeleton className="h-40 w-full" />;
  }

  if (!me) {
    return (
      <div className="rounded-lg border border-border p-8 text-center">
        <h1 className="text-2xl font-semibold">개발자의 지식이 모두의 성장이 됩니다</h1>
        <p className="mt-2 text-text-secondary">로그인하면 인기 질문과 Ward 업데이트를 볼 수 있습니다.</p>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      <h1 className="text-xl font-semibold">인기 질문</h1>
      {dashboardLoading && <Skeleton className="h-24 w-full" />}
      <ul className="space-y-3">
        {dashboard?.popularQuestions.map((question) => (
          <li key={question.id} className="rounded-lg border border-border p-4">
            <div className="flex items-center gap-2">
              <StatusBadge status={question.status} />
              <span className="font-medium">{question.title}</span>
            </div>
            {question.tags.length > 0 && (
              <div className="mt-2 flex gap-1">
                {question.tags.map((tag) => (
                  <TagChip key={tag} name={tag} />
                ))}
              </div>
            )}
          </li>
        ))}
        {dashboard && dashboard.popularQuestions.length === 0 && (
          <li className="rounded-lg border border-border p-4 text-text-secondary">아직 질문이 없습니다.</li>
        )}
      </ul>
    </div>
  );
}
