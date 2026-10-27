"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { tokenApi, tokenKeys } from "../api/token.api";
import type { IssueTokenInput } from "../api/token.types";

export function useMyTokens(enabled: boolean) {
  return useQuery({ queryKey: tokenKeys.mine, queryFn: tokenApi.list, enabled });
}

export function useIssueToken() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: IssueTokenInput) => tokenApi.issue(input),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: tokenKeys.mine }),
  });
}

export function useRevokeToken() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (tokenId: number) => tokenApi.revoke(tokenId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: tokenKeys.mine }),
  });
}
