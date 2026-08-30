"use client";

import { useRequireAuth } from "@/features/auth/hooks/useRequireAuth";
import { useMyWatches } from "@/features/watch/hooks/useMyWatches";
import { WatchedQuestionList } from "@/features/watch/ui/WatchedQuestionList";
import { Skeleton } from "@/shared/ui/Skeleton";

export default function WatchingPage() {
  const { isLoading: authLoading } = useRequireAuth();
  const { data: watches, isLoading } = useMyWatches(true);

  if (authLoading || isLoading) {
    return <Skeleton className="h-40 w-full" />;
  }

  return (
    <div className="space-y-4">
      <h1 className="text-xl font-semibold">Watching</h1>
      <WatchedQuestionList questions={watches ?? []} emptyMessage="아직 Watch 중인 질문이 없습니다." />
    </div>
  );
}
