import { httpClient } from "@/shared/api/http-client";
import type { Dashboard, FlowCard } from "./dashboard.types";

export const dashboardApi = {
  get: () => httpClient.get<Dashboard>("/api/v1/dashboard"),
  flow: (limit = 5) => httpClient.get<FlowCard[]>(`/api/v1/flow?limit=${limit}`),
};
