"use client";

import { use, useState } from "react";
import { useSession } from "@/features/auth/hooks/useSession";
import { useUserProfile } from "@/entities/user/hooks/useUserProfile";
import { useUserReputation } from "@/entities/user/hooks/useUserReputation";
import { AnswerCard } from "@/features/answer/ui/AnswerCard";
import { useMyWatches } from "@/features/watch/hooks/useMyWatches";
import { WatchedQuestionList } from "@/features/watch/ui/WatchedQuestionList";
import { useMySaves } from "@/features/save/hooks/useMySaves";
import { SavedQuestionList } from "@/features/save/ui/SavedQuestionList";
import { useMyFollowing } from "@/features/follow/hooks/useMyFollowing";
import { FollowUserButton } from "@/features/follow/ui/FollowUserButton";
import { FollowingList } from "@/features/follow/ui/FollowingList";
import { useBadges } from "@/features/badge/hooks/useBadges";
import { BadgeList } from "@/features/badge/ui/BadgeList";
import { RequestDirectAskPanel } from "@/features/direct-ask/ui/RequestDirectAskPanel";
import { DirectAskSettingsToggle } from "@/features/direct-ask/ui/DirectAskSettingsToggle";
import { AccountDangerZone } from "@/features/auth/ui/AccountDangerZone";
import { QuestionList } from "@/widgets/question-feed/QuestionList";
import { TagChip } from "@/shared/ui/TagChip";
import { Skeleton } from "@/shared/ui/Skeleton";
import { EmptyState } from "@/shared/ui/EmptyState";
import { SideNav } from "@/widgets/side-nav/SideNav";
import { useNotifications } from "@/features/notification/hooks/useNotifications";
import {
  SegmentedControl,
  type SegmentOption,
} from "@/shared/ui/SegmentedControl";
import Link from "next/link";

type ProfileTab = "questions" | "answers" | "activity";

/** Publicly readable (Phase 30, ADR-0042) — `RequestDirectAskPanel`/`FollowUserButton` both
 * self-guard on `!me`, and every private-list section is already gated on `isOwnProfile`
 * (itself false without `me`), so this page needs no auth gate of its own. */
export default function UserProfilePage({ params }: PageProps<"/users/[id]">) {
  const { id } = use(params);
  const userId = Number(id);
  const { data: me } = useSession();
  const {
    data: profile,
    isLoading: profileLoading,
    isError,
  } = useUserProfile(userId);
  const { data: reputation } = useUserReputation(userId);
  const isOwnProfile = Boolean(me && me.id === userId);
  const { data: watches } = useMyWatches(isOwnProfile);
  const { data: saves } = useMySaves(isOwnProfile);
  const { data: following } = useMyFollowing(isOwnProfile);
  const { data: badges } = useBadges(userId);
  const [tab, setTab] = useState<ProfileTab>("questions");
  const { data: notifications } = useNotifications(Boolean(me));

  if (profileLoading) {
    return (
      <div className="space-y-4">
        <Skeleton className="h-16 w-full" />
        <Skeleton className="h-40 w-full" />
      </div>
    );
  }

  if (isError || !profile) {
    return <p className="text-sm text-danger">사용자를 찾을 수 없습니다.</p>;
  }

  // Someone else's profile (or a logged-out view) has no activity tab to land on.
  const currentTab: ProfileTab =
    tab === "activity" && !isOwnProfile ? "questions" : tab;

  const tabs: SegmentOption<ProfileTab>[] = [
    { value: "questions", label: "질문", count: profile.questions.length },
    { value: "answers", label: "답변", count: profile.answers.length },
    ...(isOwnProfile ? [{ value: "activity" as const, label: "내 활동" }] : []),
  ];
  const stats = reputation
    ? [
        { label: "평판", value: reputation.score, tone: "" },
        { label: "질문", value: reputation.questionCount, tone: "" },
        { label: "답변", value: reputation.answerCount, tone: "" },
        {
          label: "채택된 답변",
          value: reputation.acceptedAnswerCount,
          tone: "success",
        },
        { label: "Super Answer", value: reputation.superAnswerCount, tone: "" },
      ]
    : [];

  return (
    // Web design v2 (ADR-0071): profile card with stat tiles, then the lists behind a switch.
    <div className="grid grid-cols-1 gap-8 xl:grid-cols-[200px_minmax(0,1fr)]">
      <div className="hidden xl:block">
        <div className="sticky top-28">
          <SideNav
            wardChangeCount={0}
            userId={me?.id}
            unreadNotificationCount={
              notifications?.filter((n) => !n.isRead).length ?? 0
            }
          />
        </div>
      </div>
      <div className="min-w-0 space-y-6">
        <header className="space-y-5 rounded-3xl border border-border bg-surface px-5 py-6 sm:px-8 sm:py-7">
          <div className="flex flex-wrap items-center gap-x-6 gap-y-4">
            <span
              aria-hidden="true"
              className="grid size-20 shrink-0 place-items-center rounded-full bg-brand-subtle text-3xl font-bold text-brand-on-subtle"
            >
              {profile.nickname.slice(0, 1).toUpperCase()}
            </span>
            <div className="min-w-0 flex-1 basis-60">
              <h1 className="truncate text-[28px] leading-tight font-bold tracking-tight">
                {profile.nickname}
              </h1>
              <p className="mt-1 text-[13px] text-text-secondary">
                팔로우 태그 {profile.followedTags.length} · 소속 조직{" "}
                {profile.organizations.length}
              </p>
            </div>
            <div className="flex flex-wrap items-center gap-2">
              {isOwnProfile ? (
                me && <DirectAskSettingsToggle accepts={me.acceptsDirectAsk} />
              ) : (
                <RequestDirectAskPanel targetUserId={userId} />
              )}
              <FollowUserButton userId={userId} />
            </div>
          </div>
          {stats.length > 0 && (
            <dl className="grid grid-cols-2 gap-2.5 sm:grid-cols-5">
              {stats.map((stat) => (
                <div
                  key={stat.label}
                  className={
                    stat.tone === "success"
                      ? "rounded-[18px] bg-success-subtle px-4 py-3"
                      : "rounded-[18px] bg-surface-subtle px-4 py-3"
                  }
                >
                  <dt
                    className={
                      stat.tone === "success"
                        ? "text-xs text-success"
                        : "text-xs text-text-secondary"
                    }
                  >
                    {stat.label}
                  </dt>
                  <dd
                    className={
                      stat.tone === "success"
                        ? "font-mono text-[22px] font-semibold text-success"
                        : "font-mono text-[22px] font-semibold"
                    }
                  >
                    {stat.value}
                  </dd>
                </div>
              ))}
            </dl>
          )}
          <BadgeList
            badges={badges ?? []}
            emptyMessage="아직 획득한 배지가 없습니다."
          />
        </header>

        <div className="grid grid-cols-1 gap-7 lg:grid-cols-[minmax(0,1fr)_300px]">
          <div className="min-w-0 space-y-4">
            <SegmentedControl
              options={tabs}
              value={currentTab}
              onChange={setTab}
              label="프로필 목록"
            />

            {currentTab === "questions" && (
              <QuestionList
                questions={profile.questions}
                emptyMessage="작성한 질문이 없습니다."
              />
            )}

            {currentTab === "answers" &&
              (profile.answers.length === 0 ? (
                <EmptyState message="작성한 답변이 없습니다." />
              ) : (
                <ul className="space-y-4">
                  {profile.answers.map((answer) => (
                    <AnswerCard
                      key={answer.id}
                      answer={answer}
                      questionHref={`/questions/${answer.questionId}`}
                    />
                  ))}
                </ul>
              ))}

            {/* The private lists share one "only you" notice instead of repeating it per list. */}
            {currentTab === "activity" && isOwnProfile && (
              <section
                aria-labelledby="my-activity-heading"
                className="space-y-5"
              >
                <div className="flex flex-wrap items-baseline justify-between gap-2">
                  <h2
                    id="my-activity-heading"
                    className="text-base font-semibold text-text-primary"
                  >
                    내 활동
                  </h2>
                  <span className="text-[13px] text-text-secondary">
                    본인에게만 보이는 목록입니다.
                  </span>
                </div>
                <div className="space-y-3">
                  <h3 className="text-sm font-semibold text-text-primary">
                    Watching{" "}
                    <span className="font-mono text-text-secondary">
                      {watches?.length ?? 0}
                    </span>
                  </h3>
                  <WatchedQuestionList
                    questions={watches ?? []}
                    emptyMessage="아직 Watch 중인 질문이 없습니다."
                  />
                </div>
                <div className="space-y-3">
                  <h3 className="text-sm font-semibold text-text-primary">
                    Saved{" "}
                    <span className="font-mono text-text-secondary">
                      {saves?.length ?? 0}
                    </span>
                  </h3>
                  <SavedQuestionList
                    questions={saves ?? []}
                    emptyMessage="아직 저장한 질문이 없습니다."
                  />
                </div>
                <div className="space-y-3">
                  <h3 className="text-sm font-semibold text-text-primary">
                    Following{" "}
                    <span className="font-mono text-text-secondary">
                      {following?.length ?? 0}
                    </span>
                  </h3>
                  <FollowingList
                    users={following ?? []}
                    emptyMessage="아직 팔로우하는 사용자가 없습니다."
                  />
                </div>
              </section>
            )}
          </div>

          <aside className="min-w-0 space-y-4">
            {profile.followedTags.length > 0 && (
              <section className="space-y-3 rounded-[20px] border border-border bg-surface p-5">
                <h2 className="text-sm font-semibold text-text-primary">
                  팔로우 태그
                </h2>
                <div className="flex flex-wrap gap-1.5">
                  {profile.followedTags.map((tag) => (
                    <TagChip key={tag.id} name={tag.name} />
                  ))}
                </div>
              </section>
            )}

            {profile.organizations.length > 0 && (
              <section className="space-y-3 rounded-[20px] border border-border bg-surface p-5">
                <h2 className="text-sm font-semibold text-text-primary">
                  소속 조직
                </h2>
                <div className="flex flex-wrap gap-2">
                  {profile.organizations.map((organization) => (
                    <Link
                      key={organization.id}
                      href={`/organizations/${organization.id}`}
                      className="inline-flex h-7 items-center gap-1 rounded-full bg-surface-subtle px-3 text-xs font-medium text-text-primary hover:text-brand"
                    >
                      {organization.name}
                      {organization.verified && (
                        <span className="text-success">✓</span>
                      )}
                    </Link>
                  ))}
                </div>
              </section>
            )}
          </aside>
        </div>

        {isOwnProfile && <AccountDangerZone />}
      </div>
    </div>
  );
}
