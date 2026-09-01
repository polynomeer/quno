"use client";

import { useUpdateDirectAskSettings } from "../hooks/useUpdateDirectAskSettings";
import { Button } from "@/shared/ui/Button";

/** Self-service (PUT /me/direct-ask-settings) — defaults to false (spam prevention), so this is
 * how a user opts in. Only ever shown on your own profile. */
export function DirectAskSettingsToggle({ accepts }: { accepts: boolean }) {
  const updateSettings = useUpdateDirectAskSettings();

  return (
    <div className="flex items-center gap-2 text-sm">
      <span className="text-text-secondary">Direct Ask 수신</span>
      <Button
        variant={accepts ? "secondary" : "primary"}
        onClick={() => updateSettings.mutate(!accepts)}
        disabled={updateSettings.isPending}
      >
        {accepts ? "받는 중 — 끄기" : "꺼짐 — 켜기"}
      </Button>
    </div>
  );
}
