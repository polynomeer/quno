import { httpClient } from "@/shared/api/http-client";
import type { Badge } from "./badge.types";

export const badgeApi = {
  list: (userId: number) => httpClient.get<Badge[]>(`/api/v1/users/${userId}/badges`),
};
