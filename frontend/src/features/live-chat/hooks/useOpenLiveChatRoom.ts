"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { liveChatApi } from "../api/live-chat.api";
import { liveChatKeys } from "../api/live-chat.keys";

/** Find-or-create — always safe to call even if a room might already exist (mirrors the
 * backend's OpenLiveChatRoomUseCase idempotency). */
export function useOpenLiveChatRoom(questionId: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () => liveChatApi.openRoom(questionId),
    onSuccess: (room) => {
      queryClient.setQueryData(liveChatKeys.room(questionId), room);
    },
  });
}
