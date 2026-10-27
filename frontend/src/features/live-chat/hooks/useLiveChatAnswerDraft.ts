"use client";

import { useMutation } from "@tanstack/react-query";
import { liveChatApi } from "../api/live-chat.api";

/** A mutation, not a query: the draft is built on demand and never cached (ADR-0073). */
export function useLiveChatAnswerDraft(roomId: number | null) {
  return useMutation({
    mutationFn: (messageIds: string[]) =>
      liveChatApi.answerDraft(roomId as number, messageIds),
  });
}
