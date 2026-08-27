"use client";

import { useQuery } from "@tanstack/react-query";
import { userApi } from "../api/user.api";
import { userKeys } from "../api/user.keys";

export function useUserReputation(id: number) {
  return useQuery({
    queryKey: userKeys.reputation(id),
    queryFn: () => userApi.getReputation(id),
  });
}
