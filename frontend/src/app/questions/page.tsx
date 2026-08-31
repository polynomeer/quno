"use client";

import { Suspense, useMemo, useState, type FormEvent } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { useRequireAuth } from "@/features/auth/hooks/useRequireAuth";
import { useSearch } from "@/features/search/hooks/useSearch";
import { SearchFilters } from "@/features/search/ui/SearchFilters";
import { QuestionList } from "@/widgets/question-feed/QuestionList";
import { Input } from "@/shared/ui/Input";
import { Button } from "@/shared/ui/Button";
import { Skeleton } from "@/shared/ui/Skeleton";
import type { QuestionStatus } from "@/shared/ui/StatusBadge";
import type { SearchSort } from "@/features/search/api/search.types";

function QuestionsSearchContent() {
  const { isLoading: authLoading } = useRequireAuth();
  const router = useRouter();
  const searchParams = useSearchParams();
  const q = searchParams.get("q") ?? "";
  const [draft, setDraft] = useState(q);
  const sort: SearchSort = searchParams.get("sort") === "score" ? "score" : "relevance";
  const { data: results, isLoading, isError } = useSearch(q, sort);

  const selectedTags = useMemo(
    () => (searchParams.get("tags") ?? "").split(",").filter(Boolean),
    [searchParams],
  );
  const selectedStatuses = useMemo(
    () => (searchParams.get("status") ?? "").split(",").filter(Boolean) as QuestionStatus[],
    [searchParams],
  );

  function updateListParam(name: string, values: string[]) {
    const params = new URLSearchParams(searchParams);
    if (values.length > 0) {
      params.set(name, values.join(","));
    } else {
      params.delete(name);
    }
    router.push(`/questions?${params.toString()}`);
  }

  function toggleTag(tag: string) {
    updateListParam("tags", selectedTags.includes(tag) ? selectedTags.filter((t) => t !== tag) : [...selectedTags, tag]);
  }

  function toggleStatus(status: QuestionStatus) {
    updateListParam(
      "status",
      selectedStatuses.includes(status) ? selectedStatuses.filter((s) => s !== status) : [...selectedStatuses, status],
    );
  }

  function handleSortChange(nextSort: SearchSort) {
    const params = new URLSearchParams(searchParams);
    if (nextSort === "score") {
      params.set("sort", nextSort);
    } else {
      params.delete("sort");
    }
    router.push(`/questions?${params.toString()}`);
  }

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

  const availableTags = useMemo(
    () => Array.from(new Set((results ?? []).flatMap((r) => r.tags))).sort(),
    [results],
  );

  const filteredResults = useMemo(
    () =>
      (results ?? []).filter((result) => {
        if (selectedTags.length > 0 && !selectedTags.every((tag) => result.tags.includes(tag))) return false;
        if (selectedStatuses.length > 0 && !selectedStatuses.includes(result.status)) return false;
        return true;
      }),
    [results, selectedTags, selectedStatuses],
  );

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
          <SearchFilters
            availableTags={availableTags}
            selectedTags={selectedTags}
            onToggleTag={toggleTag}
            selectedStatuses={selectedStatuses}
            onToggleStatus={toggleStatus}
          />
          <div className="flex items-center justify-between">
            <p className="text-sm text-text-secondary">{filteredResults.length}개 결과</p>
            <label className="flex items-center gap-1 text-sm text-text-secondary">
              sort:
              <select
                className="rounded-md border border-border bg-surface px-2 py-1"
                value={sort}
                onChange={(event) => handleSortChange(event.target.value as SearchSort)}
              >
                <option value="relevance">Relevance</option>
                <option value="score">Score</option>
              </select>
            </label>
          </div>
          <QuestionList
            questions={filteredResults}
            emptyMessage="검색 결과가 없습니다. 검색어를 완화하거나 필터를 줄여 보세요."
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
