"use client";

import { useQuery } from "@tanstack/react-query";
import { directAskApi } from "../api/direct-ask.api";
import { directAskKeys } from "../api/direct-ask.keys";

export function useMyDirectAsks(role: "sent" | "received", enabled = true) {
  return useQuery({
    queryKey: directAskKeys.mine(role),
    queryFn: () => directAskApi.mine(role),
    enabled,
  });
}
