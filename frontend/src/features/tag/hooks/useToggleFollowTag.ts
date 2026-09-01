"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { tagApi } from "@/entities/tag/api/tag.api";
import { userKeys } from "@/entities/user/api/user.keys";

export function useToggleFollowTag(tagId: number, viewerId: number | undefined) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (isFollowing: boolean) => (isFollowing ? tagApi.unfollow(tagId) : tagApi.follow(tagId)),
    onSuccess: () => {
      if (viewerId) {
        queryClient.invalidateQueries({ queryKey: userKeys.profile(viewerId) });
      }
    },
  });
}
