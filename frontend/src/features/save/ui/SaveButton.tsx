"use client";

import { useSession } from "@/features/auth/hooks/useSession";
import { useMySaves } from "../hooks/useMySaves";
import { useToggleSave } from "../hooks/useToggleSave";
import { Button } from "@/shared/ui/Button";

/** Hidden for anonymous viewers (Phase 29, ADR-0041 — reading is public, saving still isn't). */
export function SaveButton({ questionId }: { questionId: number }) {
  const { data: me } = useSession();
  const { data: saves, isLoading } = useMySaves(Boolean(me));
  const toggleSave = useToggleSave(questionId);
  const isSaved = Boolean(saves?.some((s) => s.questionId === questionId));

  if (!me) {
    return null;
  }

  return (
    <Button
      variant={isSaved ? "secondary" : "primary"}
      onClick={() => toggleSave.mutate(isSaved)}
      disabled={isLoading || toggleSave.isPending}
    >
      {isSaved ? "Saved" : "Save"}
    </Button>
  );
}
