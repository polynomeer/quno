"use client";

import { useRequireAuth } from "@/features/auth/hooks/useRequireAuth";
import { useMyWatches } from "@/features/watch/hooks/useMyWatches";
import { WatchedQuestionList } from "@/features/watch/ui/WatchedQuestionList";
import Link from "next/link";
import { Skeleton } from "@/shared/ui/Skeleton";

export default function WatchingPage() {
  const { isLoading: authLoading } = useRequireAuth();
  const { data: watches, isLoading } = useMyWatches(true);

  if (authLoading || isLoading) {
    return <Skeleton className="h-40 w-full" />;
  }

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <h1 className="text-[32px] leading-10 font-bold tracking-tight">Watching</h1>
        <Link href="/my-stack" className="text-sm font-semibold text-brand hover:underline">
          내 스택으로 Watch할 질문 찾기 →
        </Link>
      </div>
      <WatchedQuestionList questions={watches ?? []} emptyMessage="아직 Watch 중인 질문이 없습니다." />
    </div>
  );
}
