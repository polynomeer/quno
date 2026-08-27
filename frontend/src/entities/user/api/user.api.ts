import { httpClient } from "@/shared/api/http-client";
import type { UserProfile, UserReputation } from "../model/user-profile.types";

export const userApi = {
  getProfile: (id: number) => httpClient.get<UserProfile>(`/api/v1/users/${id}/profile`),
  getReputation: (id: number) => httpClient.get<UserReputation>(`/api/v1/users/${id}/reputation`),
};
