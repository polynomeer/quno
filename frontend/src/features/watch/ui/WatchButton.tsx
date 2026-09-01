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
      variant={isWatching ? "secondary" : "primary"}
      onClick={() => toggleWatch.mutate(isWatching)}
      disabled={isLoading || toggleWatch.isPending}
    >
      {isWatching ? "Watching" : "Watch"}
    </Button>
  );
}
