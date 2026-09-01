"use client";

import { useQuery } from "@tanstack/react-query";
import { liveChatApi } from "../api/live-chat.api";
import { liveChatKeys } from "../api/live-chat.keys";
import { ApiError } from "@/shared/api/api-error";

/** No room yet is a normal state (backend 404s LiveChatRoomNotFoundException), not an error —
 * resolved to `null` so the panel can offer to start one instead of showing an error banner
 * (mirrors useCluster.ts). */
export function useLiveChatRoom(questionId: number) {
  return useQuery({
    queryKey: liveChatKeys.room(questionId),
    queryFn: async () => {
      try {
        return await liveChatApi.getRoom(questionId);
      } catch (error) {
        if (error instanceof ApiError && error.status === 404) {
          return null;
        }
        throw error;
      }
    },
  });
}
