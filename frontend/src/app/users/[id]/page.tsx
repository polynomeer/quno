"use client";

import { use } from "react";
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
import Link from "next/link";

/** Publicly readable (Phase 30, ADR-0042) — `RequestDirectAskPanel`/`FollowUserButton` both
 * self-guard on `!me`, and every private-list section is already gated on `isOwnProfile`
 * (itself false without `me`), so this page needs no auth gate of its own. */
export default function UserProfilePage({ params }: PageProps<"/users/[id]">) {
  const { id } = use(params);
  const userId = Number(id);
  const { data: me } = useSession();
  const { data: profile, isLoading: profileLoading, isError } = useUserProfile(userId);
  const { data: reputation } = useUserReputation(userId);
  const isOwnProfile = Boolean(me && me.id === userId);
  const { data: watches } = useMyWatches(isOwnProfile);
  const { data: saves } = useMySaves(isOwnProfile);
  const { data: following } = useMyFollowing(isOwnProfile);
  const { data: badges } = useBadges(userId);

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

  return (
    <div className="space-y-8">
      <header className="space-y-3">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <h1 className="text-2xl font-bold tracking-tight">{profile.nickname}</h1>
          <div className="flex flex-wrap items-center gap-2">
            {isOwnProfile ? (
              me && <DirectAskSettingsToggle accepts={me.acceptsDirectAsk} />
            ) : (
              <RequestDirectAskPanel targetUserId={userId} />
            )}
            <FollowUserButton userId={userId} />
          </div>
        </div>
        {reputation && (
          <div className="flex flex-wrap items-center gap-4 text-[13px] text-text-secondary">
            <span className="text-lg font-semibold text-text-primary">
              <span className="font-mono">{reputation.score}</span> 평판
            </span>
            <span>
              질문 <span className="font-mono">{reputation.questionCount}</span>
            </span>
            <span>
              답변 <span className="font-mono">{reputation.answerCount}</span>
            </span>
            <span>
              채택된 답변 <span className="font-mono">{reputation.acceptedAnswerCount}</span>
            </span>
            <span>
              Super Answer <span className="font-mono">{reputation.superAnswerCount}</span>
            </span>
          </div>
        )}
        <BadgeList badges={badges ?? []} emptyMessage="아직 획득한 배지가 없습니다." />
      </header>

      {/* The three private lists share one group and one "only you" notice instead of repeating
          it per list. */}
      {isOwnProfile && (
        <section aria-labelledby="my-activity-heading" className="space-y-5">
          <div className="flex flex-wrap items-baseline justify-between gap-2">
            <h2 id="my-activity-heading" className="text-base font-semibold text-text-primary">
              내 활동
            </h2>
            <span className="text-[13px] text-text-secondary">본인에게만 보이는 목록입니다.</span>
          </div>
          <div className="grid gap-5 lg:grid-cols-3">
            <div className="space-y-3">
              <h3 className="text-sm font-semibold text-text-primary">
                Watching <span className="font-mono text-text-secondary">{watches?.length ?? 0}</span>
              </h3>
              <WatchedQuestionList questions={watches ?? []} emptyMessage="아직 Watch 중인 질문이 없습니다." />
            </div>
            <div className="space-y-3">
              <h3 className="text-sm font-semibold text-text-primary">
                Saved <span className="font-mono text-text-secondary">{saves?.length ?? 0}</span>
              </h3>
              <SavedQuestionList questions={saves ?? []} emptyMessage="아직 저장한 질문이 없습니다." />
            </div>
            <div className="space-y-3">
              <h3 className="text-sm font-semibold text-text-primary">
                Following <span className="font-mono text-text-secondary">{following?.length ?? 0}</span>
              </h3>
              <FollowingList users={following ?? []} emptyMessage="아직 팔로우하는 사용자가 없습니다." />
            </div>
          </div>
        </section>
      )}

      {profile.followedTags.length > 0 && (
        <section className="space-y-3 rounded-xl border border-border bg-surface p-5">
          <h2 className="text-sm font-semibold text-text-primary">팔로우 태그</h2>
          <div className="flex flex-wrap gap-1.5">
            {profile.followedTags.map((tag) => (
              <TagChip key={tag.id} name={tag.name} />
            ))}
          </div>
        </section>
      )}

      {profile.organizations.length > 0 && (
        <section className="space-y-3 rounded-xl border border-border bg-surface p-5">
          <h2 className="text-sm font-semibold text-text-primary">소속 조직</h2>
          <div className="flex flex-wrap gap-2">
            {profile.organizations.map((organization) => (
              <Link
                key={organization.id}
                href={`/organizations/${organization.id}`}
                className="inline-flex h-[22px] items-center gap-1 rounded border border-border bg-canvas px-2 text-xs font-medium text-text-primary hover:text-brand"
              >
                {organization.name}
                {organization.verified && <span className="text-success">✓</span>}
              </Link>
            ))}
          </div>
        </section>
      )}

      <section className="space-y-3">
        <h2 className="text-base font-semibold text-text-primary">
          작성한 질문 <span className="font-mono text-text-secondary">{profile.questions.length}</span>
        </h2>
        <QuestionList questions={profile.questions} emptyMessage="작성한 질문이 없습니다." />
      </section>

      <section className="space-y-3">
        <h2 className="text-base font-semibold text-text-primary">
          작성한 답변 <span className="font-mono text-text-secondary">{profile.answers.length}</span>
        </h2>
        {profile.answers.length === 0 ? (
          <EmptyState message="작성한 답변이 없습니다." />
        ) : (
          // Separate cards on purpose — same as the question page's answer list.
          <ul className="space-y-4">
            {profile.answers.map((answer) => (
              <AnswerCard key={answer.id} answer={answer} questionHref={`/questions/${answer.questionId}`} />
            ))}
          </ul>
        )}
      </section>

      {isOwnProfile && <AccountDangerZone />}
    </div>
  );
}
