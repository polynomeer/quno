"use client";

import Link from "next/link";
import { useSession } from "@/features/auth/hooks/useSession";
import { useDashboard } from "@/features/dashboard/hooks/useDashboard";
import { useFlow } from "@/features/dashboard/hooks/useFlow";
import { QuestionList } from "@/widgets/question-feed/QuestionList";
import { TrendingTagsPanel } from "@/widgets/activity-panel/TrendingTagsPanel";
import { FlowFeed } from "@/widgets/activity-panel/FlowFeed";
import { Skeleton } from "@/shared/ui/Skeleton";
import { useLocale } from "@/shared/i18n/LocaleProvider";

export default function HomePage() {
  const { t } = useLocale();
  const { data: me, isLoading: sessionLoading } = useSession();
  const { data: dashboard, isLoading: dashboardLoading } = useDashboard(Boolean(me));
  const { data: flow, isLoading: flowLoading } = useFlow(Boolean(me));

  if (sessionLoading) {
    return <Skeleton className="h-40 w-full" />;
  }

  if (!me) {
    return (
      <div className="rounded-lg border border-border p-8 text-center">
        <h1 className="text-2xl font-semibold">{t.home.guestTitle}</h1>
        <p className="mt-2 text-text-secondary">{t.home.guestSubtitle}</p>
      </div>
    );
  }

  if (dashboardLoading || !dashboard) {
    return (
      <div className="space-y-4">
        <Skeleton className="h-16 w-full" />
        <Skeleton className="h-24 w-full" />
        <Skeleton className="h-24 w-full" />
      </div>
    );
  }

  return (
    <div className="grid grid-cols-1 gap-6 lg:grid-cols-[1fr_280px]">
      <div className="space-y-8">
        {dashboard.headline && (
          <div className="rounded-lg border border-brand/30 bg-brand/5 p-4">
            {dashboard.headline.questionId ? (
              <Link href={`/questions/${dashboard.headline.questionId}`} className="font-medium hover:underline">
                {dashboard.headline.text}
              </Link>
            ) : (
              <span className="font-medium">{dashboard.headline.text}</span>
            )}
          </div>
        )}

        <section className="space-y-3">
          <h2 className="text-lg font-semibold">{t.home.popularQuestions}</h2>
          <QuestionList questions={dashboard.popularQuestions} emptyMessage={t.home.noQuestionsYet} />
        </section>

        {dashboard.followingTagsFeed.length > 0 && (
          <section className="space-y-3">
            <h2 className="text-lg font-semibold">{t.home.followingTagsFeed}</h2>
            <QuestionList questions={dashboard.followingTagsFeed} emptyMessage="" />
          </section>
        )}

        {dashboard.resolvedToday.length > 0 && (
          <section className="space-y-3">
            <h2 className="text-lg font-semibold">{t.home.resolvedToday}</h2>
            <QuestionList questions={dashboard.resolvedToday} emptyMessage="" />
          </section>
        )}

        {dashboard.reopenedKnowledge.length > 0 && (
          <section className="space-y-3">
            <h2 className="text-lg font-semibold">{t.home.reopenedKnowledge}</h2>
            <QuestionList questions={dashboard.reopenedKnowledge} emptyMessage="" />
          </section>
        )}

        <section className="space-y-3">
          <h2 className="text-lg font-semibold">{t.home.flow}</h2>
          {flowLoading ? <Skeleton className="h-24 w-full" /> : <FlowFeed cards={flow ?? []} />}
        </section>
      </div>

      <aside>
        <TrendingTagsPanel tags={dashboard.trendingTags} spikes={dashboard.trendingErrors} />
      </aside>
    </div>
  );
}
