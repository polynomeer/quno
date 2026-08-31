"use client";

import { useRequireAuth } from "@/features/auth/hooks/useRequireAuth";
import { useMySaves } from "@/features/save/hooks/useMySaves";
import { SavedQuestionList } from "@/features/save/ui/SavedQuestionList";
import { Skeleton } from "@/shared/ui/Skeleton";

export default function SavedPage() {
  const { isLoading: authLoading } = useRequireAuth();
  const { data: saves, isLoading } = useMySaves(true);

  if (authLoading || isLoading) {
    return <Skeleton className="h-40 w-full" />;
  }

  return (
    <div className="space-y-4">
      <h1 className="text-xl font-semibold">Saved</h1>
      <SavedQuestionList questions={saves ?? []} emptyMessage="아직 저장한 질문이 없습니다." />
    </div>
  );
}
