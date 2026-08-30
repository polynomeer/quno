"use client";

import { useQuery } from "@tanstack/react-query";
import { voteApi } from "../api/vote.api";
import { voteKeys } from "../api/vote.keys";

export function useMyVotes(enabled: boolean) {
  return useQuery({
    queryKey: voteKeys.mine,
    queryFn: voteApi.myVotes,
    enabled,
  });
}
