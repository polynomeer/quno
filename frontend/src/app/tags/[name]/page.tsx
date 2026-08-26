"use client";

import { use } from "react";
import { useRequireAuth } from "@/features/auth/hooks/useRequireAuth";
import { useSearch } from "@/features/search/hooks/useSearch";
import { QuestionList } from "@/widgets/question-feed/QuestionList";
import { Skeleton } from "@/shared/ui/Skeleton";

/**
 * There is no "questions by tag" endpoint (Tag only has id/name/slug — see ADR-0021), so this
 * approximates it: fetch GET /search?q={name} and keep only results whose tags[] contains the
 * exact tag name, filtering out the plain full-text matches search also returns.
 */
export default function TagDetailPage({ params }: PageProps<"/tags/[name]">) {
  const { name } = use(params);
  const tagName = decodeURIComponent(name);
  const { isLoading: authLoading } = useRequireAuth();
  const { data: results, isLoading } = useSearch(tagName);

  if (authLoading || isLoading) {
    return <Skeleton className="h-40 w-full" />;
  }

  const questions = (results ?? []).filter((question) => question.tags.includes(tagName));

  return (
    <div className="space-y-6">
      <h1 className="text-xl font-semibold">#{tagName}</h1>
      <QuestionList questions={questions} emptyMessage="이 태그가 달린 질문이 없습니다." />
    </div>
  );
}
