/** Mirrors backend OrganizationResponse (interfaces/api/organization/OrganizationResponses.kt). */
export interface Organization {
  id: number;
  name: string;
  description: string | null;
  createdBy: number;
  memberCount: number;
  /** Non-null only for a Verified organization (Phase 23, ADR-0035). */
  emailDomain: string | null;
  verified: boolean;
  createdAt: string;
}

export interface EmailDomainVerification {
  email: string;
  expiresAt: string;
}
