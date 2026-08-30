"use client";

import { use } from "react";
import { useRequireAuth } from "@/features/auth/hooks/useRequireAuth";
import { useUserProfile } from "@/entities/user/hooks/useUserProfile";
import { useUserReputation } from "@/entities/user/hooks/useUserReputation";
import { AnswerCard } from "@/features/answer/ui/AnswerCard";
import { useMyWatches } from "@/features/watch/hooks/useMyWatches";
import { WatchedQuestionList } from "@/features/watch/ui/WatchedQuestionList";
import { QuestionList } from "@/widgets/question-feed/QuestionList";
import { TagChip } from "@/shared/ui/TagChip";
import { Skeleton } from "@/shared/ui/Skeleton";

export default function UserProfilePage({ params }: PageProps<"/users/[id]">) {
  const { id } = use(params);
  const userId = Number(id);
  const { me, isLoading: authLoading } = useRequireAuth();
  const { data: profile, isLoading: profileLoading, isError } = useUserProfile(userId);
  const { data: reputation } = useUserReputation(userId);
  const isOwnProfile = Boolean(me && me.id === userId);
  const { data: watches } = useMyWatches(isOwnProfile);

  if (authLoading || profileLoading) {
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
      <header className="space-y-2">
        <h1 className="text-2xl font-semibold">{profile.nickname}</h1>
        {reputation && (
          <div className="flex flex-wrap items-center gap-4 text-sm text-text-secondary">
            <span className="text-lg font-semibold text-text-primary">{reputation.score} 평판</span>
            <span>질문 {reputation.questionCount}</span>
            <span>답변 {reputation.answerCount}</span>
            <span>채택된 답변 {reputation.acceptedAnswerCount}</span>
            <span>Super Answer {reputation.superAnswerCount}</span>
          </div>
        )}
      </header>

      {isOwnProfile && (
        <section className="space-y-3">
          <h2 className="text-lg font-semibold">Watching ({watches?.length ?? 0})</h2>
          <p className="text-xs text-text-secondary">본인에게만 보이는 목록입니다.</p>
          <WatchedQuestionList questions={watches ?? []} emptyMessage="아직 Watch 중인 질문이 없습니다." />
        </section>
      )}

      {profile.followedTags.length > 0 && (
        <section className="space-y-2">
          <h2 className="text-sm font-semibold text-text-secondary">팔로우 태그</h2>
          <div className="flex flex-wrap gap-1">
            {profile.followedTags.map((tag) => (
              <TagChip key={tag.id} name={tag.name} />
            ))}
          </div>
        </section>
      )}

      <section className="space-y-3">
        <h2 className="text-lg font-semibold">작성한 질문 ({profile.questions.length})</h2>
        <QuestionList questions={profile.questions} emptyMessage="작성한 질문이 없습니다." />
      </section>

      <section className="space-y-3">
        <h2 className="text-lg font-semibold">작성한 답변 ({profile.answers.length})</h2>
        {profile.answers.length === 0 ? (
          <p className="text-sm text-text-secondary">작성한 답변이 없습니다.</p>
        ) : (
          <ul className="space-y-3">
            {profile.answers.map((answer) => (
              <AnswerCard key={answer.id} answer={answer} questionHref={`/questions/${answer.questionId}`} />
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}
