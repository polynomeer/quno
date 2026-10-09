"use client";

import { useSession } from "@/features/auth/hooks/useSession";
import { useMyWatches } from "../hooks/useMyWatches";
import { useToggleWatch } from "../hooks/useToggleWatch";
import { Button } from "@/shared/ui/Button";

/** Hidden for anonymous viewers (Phase 29, ADR-0041 — reading is public, watching still isn't). */
export function WatchButton({ questionId }: { questionId: number }) {
  const { data: me } = useSession();
  const { data: watches, isLoading } = useMyWatches(Boolean(me));
  const toggleWatch = useToggleWatch(questionId);
  const isWatching = Boolean(watches?.some((w) => w.questionId === questionId));

  if (!me) {
    return null;
  }

  return (
    <Button
      variant="ghost"
      aria-pressed={isWatching}
      // Toggle styling from the design canvas: an outlined chip that turns brand-subtle when on.
      className={
        isWatching
          ? "border border-brand bg-brand-subtle font-semibold text-brand-on-subtle hover:bg-brand-subtle hover:text-brand-on-subtle"
          : "border border-border-strong bg-surface"
      }
      onClick={() => toggleWatch.mutate(isWatching)}
      disabled={isLoading || toggleWatch.isPending}
    >
      {isWatching ? "Watching" : "Watch"}
    </Button>
  );
}
