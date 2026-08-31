"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { followApi } from "../api/follow.api";
import { followKeys } from "../api/follow.keys";

export function useToggleFollow(userId: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (isFollowing: boolean) => (isFollowing ? followApi.unfollow(userId) : followApi.follow(userId)),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: followKeys.mine });
    },
  });
}
