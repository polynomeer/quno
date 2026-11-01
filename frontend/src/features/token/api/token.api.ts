import { httpClient } from "@/shared/api/http-client";
import type { IssuedToken, IssueTokenInput, PersonalAccessToken } from "./token.types";

export const tokenApi = {
  list: () => httpClient.get<PersonalAccessToken[]>("/api/v1/me/tokens"),
  issue: (input: IssueTokenInput) => httpClient.post<IssuedToken>("/api/v1/me/tokens", input),
  revoke: (tokenId: number) => httpClient.delete<void>(`/api/v1/me/tokens/${tokenId}`),
};

export const tokenKeys = {
  mine: ["tokens", "me"] as const,
};
