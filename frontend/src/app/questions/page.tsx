"use client";

import { Suspense, useState, type FormEvent } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { useRequireAuth } from "@/features/auth/hooks/useRequireAuth";
import { useSearch } from "@/features/search/hooks/useSearch";
import { QuestionList } from "@/widgets/question-feed/QuestionList";
import { Input } from "@/shared/ui/Input";
import { Button } from "@/shared/ui/Button";
import { Skeleton } from "@/shared/ui/Skeleton";

function QuestionsSearchContent() {
  const { isLoading: authLoading } = useRequireAuth();
  const router = useRouter();
  const searchParams = useSearchParams();
  const q = searchParams.get("q") ?? "";
  const [draft, setDraft] = useState(q);
  const { data: results, isLoading, isError } = useSearch(q);

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const params = new URLSearchParams(searchParams);
    if (draft.trim()) {
      params.set("q", draft.trim());
    } else {
      params.delete("q");
    }
    router.push(`/questions?${params.toString()}`);
  }

  if (authLoading) {
    return <Skeleton className="h-40 w-full" />;
  }

  return (
    <div className="space-y-6">
      <form onSubmit={handleSubmit} className="flex gap-2">
        <Input
          value={draft}
          onChange={(event) => setDraft(event.target.value)}
          placeholder="spring transaction coroutine..."
          className="flex-1"
        />
        <Button type="submit">Search</Button>
      </form>

      {!q && <p className="text-sm text-text-secondary">검색어를 입력하세요.</p>}

      {q && isLoading && (
        <div className="space-y-3">
          <Skeleton className="h-20 w-full" />
          <Skeleton className="h-20 w-full" />
          <Skeleton className="h-20 w-full" />
        </div>
      )}

      {q && isError && <p className="text-sm text-danger">검색 중 오류가 발생했습니다. 다시 시도해 주세요.</p>}

      {q && !isLoading && !isError && (
        <>
          <p className="text-sm text-text-secondary">{results?.length ?? 0}개 결과</p>
          <QuestionList
            questions={results ?? []}
            emptyMessage="검색 결과가 없습니다. 검색어를 완화하거나 태그를 줄여 보세요."
          />
        </>
      )}
    </div>
  );
}

export default function QuestionsSearchPage() {
  return (
    <Suspense>
      <QuestionsSearchContent />
    </Suspense>
  );
}
