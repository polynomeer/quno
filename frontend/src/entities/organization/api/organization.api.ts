import { httpClient } from "@/shared/api/http-client";
import type { EmailDomainVerification, Organization } from "../model/organization.types";

export const organizationApi = {
  search: (q?: string) => {
    const query = q ? `?q=${encodeURIComponent(q)}` : "";
    return httpClient.get<Organization[]>(`/api/v1/organizations${query}`);
  },
  get: (id: number) => httpClient.get<Organization>(`/api/v1/organizations/${id}`),
  create: (input: { name: string; description?: string }) =>
    httpClient.post<Organization>("/api/v1/organizations", input),
  join: (id: number) => httpClient.post<void>(`/api/v1/organizations/${id}/join`),
  leave: (id: number) => httpClient.delete<void>(`/api/v1/organizations/${id}/join`),
  requestEmailVerification: (email: string) =>
    httpClient.post<EmailDomainVerification>("/api/v1/organizations/verify-email", { email }),
  confirmEmailVerification: (code: string) =>
    httpClient.post<Organization>("/api/v1/organizations/verify-email/confirm", { code }),
};
