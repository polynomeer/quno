"use client";

import { useState } from "react";
import Link from "next/link";
import { useSession } from "@/features/auth/hooks/useSession";
import { useDashboard } from "@/features/dashboard/hooks/useDashboard";
import { useFlow } from "@/features/dashboard/hooks/useFlow";
import { useNotifications } from "@/features/notification/hooks/useNotifications";
import { QuestionList } from "@/widgets/question-feed/QuestionList";
import { TrendingTagsPanel } from "@/widgets/activity-panel/TrendingTagsPanel";
import { FlowFeed } from "@/widgets/activity-panel/FlowFeed";
import { SideNav } from "@/widgets/side-nav/SideNav";
import { Skeleton } from "@/shared/ui/Skeleton";
import { SegmentedControl, type SegmentOption } from "@/shared/ui/SegmentedControl";
import { useLocale } from "@/shared/i18n/LocaleProvider";

type FeedSection = "popular" | "following" | "resolved" | "reopened";

function fill(template: string, values: Record<string, string>): string {
  return template.replace(/\{(\w+)\}/g, (_, key: string) => values[key] ?? "");
}

export default function HomePage() {
  const { t } = useLocale();
  const { data: me, isLoading: sessionLoading } = useSession();
  const { data: dashboard, isLoading: dashboardLoading } = useDashboard(Boolean(me));
  const { data: flow, isLoading: flowLoading } = useFlow(Boolean(me));
  const { data: notifications } = useNotifications(Boolean(me));
  const [section, setSection] = useState<FeedSection>("popular");

  if (sessionLoading) {
    return <Skeleton className="h-40 w-full" />;
  }

  if (!me) {
    return (
      <div className="rounded-3xl border border-border bg-surface p-10 text-center">
        <h1 className="text-2xl font-bold tracking-tight">{t.home.guestTitle}</h1>
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

  // The v2 canvas's 추천/최신/미해결/내 태그 filters have no API behind them, so the switch flips
  // between the sections the dashboard actually returns (ADR-0071). Empty ones are left out,
  // except the popular list, which always shows so the page never opens on nothing.
  const sections: Record<FeedSection, { label: string; questions: typeof dashboard.popularQuestions }> = {
    popular: { label: t.home.popularQuestions, questions: dashboard.popularQuestions },
    following: { label: t.home.followingTagsFeed, questions: dashboard.followingTagsFeed },
    resolved: { label: t.home.resolvedToday, questions: dashboard.resolvedToday },
    reopened: { label: t.home.reopenedKnowledge, questions: dashboard.reopenedKnowledge },
  };
  const options: SegmentOption<FeedSection>[] = (Object.keys(sections) as FeedSection[])
    .filter((key) => key === "popular" || sections[key].questions.length > 0)
    .map((key) => ({ value: key, label: sections[key].label, count: sections[key].questions.length }));
  const current = options.some((option) => option.value === section) ? section : "popular";
  const topSpike = dashboard.trendingErrors[0];
  const unreadNotificationCount = notifications?.filter((n) => !n.isRead).length ?? 0;

  return (
    // Three columns from xl up (design.md #8: nav + main + aside at ≥1280px). Below that the left
    // nav folds away — the header's own links and mobile menu already cover the same routes.
    <div className="grid grid-cols-1 gap-8 lg:grid-cols-[minmax(0,1fr)_300px] xl:grid-cols-[200px_minmax(0,1fr)_300px]">
      <div className="hidden xl:block">
        <div className="sticky top-28">
          <SideNav
            wardChangeCount={dashboard.wardUpdates.filter((update) => !update.isRead).length}
            userId={me.id}
            unreadNotificationCount={unreadNotificationCount}
          />
        </div>
      </div>
      <div className="min-w-0 space-y-5">
        <div className="flex flex-wrap items-end justify-between gap-x-6 gap-y-3">
          <h1 className="text-[32px] leading-10 font-bold tracking-tight">{t.home.title}</h1>
          {options.length > 1 && (
            <SegmentedControl options={options} value={current} onChange={setSection} label={t.home.feedFilter} />
          )}
        </div>

        {dashboard.headline && (
          <div className="flex flex-wrap items-center gap-x-4 gap-y-2 rounded-[20px] border border-border bg-surface px-5 py-3.5">
            <span className="inline-flex items-center gap-2 font-mono text-xs font-semibold tracking-wide text-brand">
              <span aria-hidden="true" className="size-2 rounded-full bg-brand" />
              WARD
            </span>
            {dashboard.headline.questionId ? (
              <Link
                href={`/questions/${dashboard.headline.questionId}`}
                className="min-w-0 flex-1 text-sm font-medium text-text-body hover:text-brand"
              >
                {dashboard.headline.text}
              </Link>
            ) : (
              <span className="min-w-0 flex-1 text-sm font-medium text-text-body">{dashboard.headline.text}</span>
            )}
          </div>
        )}

        {topSpike && (
          <div className="flex flex-wrap items-center gap-x-4 gap-y-3 rounded-[20px] bg-brand-subtle px-5 py-4">
            <span
              aria-hidden="true"
              className="grid size-10 shrink-0 place-items-center rounded-xl bg-brand text-brand-foreground"
            >
              <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
                <path d="m3 17 6-6 4 4 8-8M14 7h7v7" />
              </svg>
            </span>
            <div className="min-w-0 flex-1 basis-64">
              <p className="text-xs font-semibold text-brand-on-subtle">{t.home.spikeLabel}</p>
              <p className="font-semibold text-text-primary">
                {fill(t.home.spikeHeadline, { tag: topSpike.name, ratio: topSpike.spikeRatio.toFixed(1) })}
              </p>
            </div>
            <Link
              href={`/tags/${topSpike.slug}`}
              className="inline-flex h-10 items-center rounded-full bg-surface px-4 text-sm font-semibold text-brand-on-subtle hover:opacity-90"
            >
              {fill(t.home.spikeLink, { tag: topSpike.name })}
            </Link>
          </div>
        )}

        <section aria-label={sections[current].label}>
          <QuestionList questions={sections[current].questions} emptyMessage={t.home.noQuestionsYet} />
        </section>

        <section id="quno-flow" className="scroll-mt-28 space-y-3 pt-3">
          <h2 className="text-[17px] font-bold">{t.home.flow}</h2>
          {flowLoading ? <Skeleton className="h-24 w-full" /> : <FlowFeed cards={flow ?? []} />}
        </section>
      </div>

      <aside className="space-y-4">
        <TrendingTagsPanel tags={dashboard.trendingTags} spikes={dashboard.trendingErrors} />
      </aside>
    </div>
  );
}
