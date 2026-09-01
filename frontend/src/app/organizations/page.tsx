"use client";

import { useState } from "react";
import { useSession } from "@/features/auth/hooks/useSession";
import { useOrganizationSearch } from "@/entities/organization/hooks/useOrganizationSearch";
import { OrganizationCard } from "@/features/organization/ui/OrganizationCard";
import { CreateOrganizationForm } from "@/features/organization/ui/CreateOrganizationForm";
import { EmailDomainVerificationPanel } from "@/features/organization/ui/EmailDomainVerificationPanel";
import { Input } from "@/shared/ui/Input";
import { Skeleton } from "@/shared/ui/Skeleton";

/** Publicly readable (Phase 30, ADR-0042) — `CreateOrganizationForm`/`EmailDomainVerificationPanel`
 * both self-guard on `!me` internally, so this page doesn't need its own auth gate. */
export default function OrganizationsPage() {
  const { data: me } = useSession();
  const [q, setQ] = useState("");
  const { data: organizations, isLoading } = useOrganizationSearch(q);

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h1 className="text-xl font-semibold">Organizations</h1>
        <CreateOrganizationForm />
      </div>

      <EmailDomainVerificationPanel viewerId={me?.id} />

      <Input value={q} onChange={(event) => setQ(event.target.value)} placeholder="조직 검색..." />

      {isLoading && <Skeleton className="h-40 w-full" />}

      {!isLoading && (
        <ul className="space-y-3">
          {(organizations ?? []).map((organization) => (
            <OrganizationCard key={organization.id} organization={organization} />
          ))}
          {organizations && organizations.length === 0 && (
            <p className="text-sm text-text-secondary">조직이 없습니다.</p>
          )}
        </ul>
      )}
    </div>
  );
}
