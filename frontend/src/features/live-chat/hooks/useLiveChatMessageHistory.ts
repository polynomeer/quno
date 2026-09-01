"use client";

import { useQuery } from "@tanstack/react-query";
import { liveChatApi } from "../api/live-chat.api";
import { liveChatKeys } from "../api/live-chat.keys";

/** Scrollback loaded once via REST before the WebSocket subscription starts delivering new
 * messages (mirrors the backend split — see LiveChatController's kdoc). Not refetched afterward;
 * useLiveChatSocket's live stream is the source of truth from that point on. */
export function useLiveChatMessageHistory(roomId: number | null, enabled: boolean) {
  return useQuery({
    queryKey: liveChatKeys.messages(roomId ?? 0),
    queryFn: () => liveChatApi.messages(roomId as number),
    enabled: enabled && roomId !== null,
    staleTime: Infinity,
  });
}
