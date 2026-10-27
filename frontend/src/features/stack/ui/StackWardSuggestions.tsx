"use client";

import { useState } from "react";
import Link from "next/link";
import type { StackWardSuggestion } from "../api/stack.types";
import { useWatchSuggestions } from "../hooks/useMyStack";
import { DependencyChips } from "./DependencyChips";
import { Button } from "@/shared/ui/Button";
import { FormError } from "@/shared/ui/FormError";
import { StatusBadge } from "@/shared/ui/StatusBadge";
import { relativeTime } from "@/shared/lib/relative-time";
import { cn } from "@/shared/lib/cn";

/**
 * Questions sharing packages with "내 스택" (Phase 63.5, ADR-0078). Nothing is ticked up front —
 * watching is the user's choice, one question at a time or several at once.
 */
export function StackWardSuggestions({ suggestions }: { suggestions: StackWardSuggestion[] }) {
  const [selected, setSelected] = useState<Set<number>>(new Set());
  const watch = useWatchSuggestions();

  if (suggestions.length === 0) {
    return <p className="text-sm text-text-secondary">내 스택과 겹치는 질문이 아직 없어요.</p>;
  }

  function toggle(questionId: number) {
    setSelected((current) => {
      const next = new Set(current);
      if (next.has(questionId)) next.delete(questionId);
      else next.add(questionId);
      return next;
    });
  }

  function watchSelected() {
    watch.mutate([...selected], { onSuccess: () => setSelected(new Set()) });
  }

  return (
    <div className="space-y-3">
      <ul className="space-y-2">
        {suggestions.map((suggestion) => {
          const checked = selected.has(suggestion.questionId);
          return (
            <li
              key={suggestion.questionId}
              className={cn(
                "flex items-start gap-3 rounded-2xl border px-4 py-3",
                checked ? "border-brand bg-brand-subtle" : "border-border bg-surface",
              )}
            >
              <input
                type="checkbox"
                checked={checked}
                onChange={() => toggle(suggestion.questionId)}
                aria-label={`${suggestion.title} Watch 선택`}
                className="mt-1 size-[18px] shrink-0 accent-brand"
              />
              <div className="min-w-0 flex-1 space-y-2">
                <div className="flex flex-wrap items-center gap-x-2.5 gap-y-1">
                  <StatusBadge status={suggestion.status} />
                  <Link href={`/questions/${suggestion.questionId}`} className="text-sm font-semibold text-text-primary hover:underline">
                    {suggestion.title}
                  </Link>
                  <span className="text-xs text-text-secondary">{relativeTime(suggestion.createdAt)}</span>
                </div>
                <DependencyChips dependencies={suggestion.sharedDependencies} />
              </div>
            </li>
          );
        })}
      </ul>
      <FormError error={watch.error} fallback="Watch하지 못했습니다." />
      <Button onClick={watchSelected} disabled={selected.size === 0 || watch.isPending}>
        {watch.isPending ? "Watch 중..." : `선택한 질문 Watch (${selected.size})`}
      </Button>
    </div>
  );
}
