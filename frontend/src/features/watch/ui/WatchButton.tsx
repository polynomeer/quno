"use client";

import { useMyWatches } from "../hooks/useMyWatches";
import { useToggleWatch } from "../hooks/useToggleWatch";
import { Button } from "@/shared/ui/Button";

export function WatchButton({ questionId }: { questionId: number }) {
  const { data: watches, isLoading } = useMyWatches(true);
  const toggleWatch = useToggleWatch(questionId);
  const isWatching = Boolean(watches?.some((w) => w.questionId === questionId));

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
