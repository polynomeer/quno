import { httpClient } from "@/shared/api/http-client";
import type { Followee } from "./follow.types";

export const followApi = {
  myFollowing: () => httpClient.get<Followee[]>("/api/v1/me/following"),
  follow: (userId: number) => httpClient.post<void>(`/api/v1/users/${userId}/follow`),
  unfollow: (userId: number) => httpClient.delete<void>(`/api/v1/users/${userId}/follow`),
};
