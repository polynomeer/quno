"use client";

import { useMySaves } from "../hooks/useMySaves";
import { useToggleSave } from "../hooks/useToggleSave";
import { Button } from "@/shared/ui/Button";

export function SaveButton({ questionId }: { questionId: number }) {
  const { data: saves, isLoading } = useMySaves(true);
  const toggleSave = useToggleSave(questionId);
  const isSaved = Boolean(saves?.some((s) => s.questionId === questionId));

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
