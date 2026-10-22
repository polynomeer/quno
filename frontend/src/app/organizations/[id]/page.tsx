"use client";

import { use } from "react";
import { useOrganization } from "@/entities/organization/hooks/useOrganization";
import { JoinOrganizationButton } from "@/features/organization/ui/JoinOrganizationButton";
import { Skeleton } from "@/shared/ui/Skeleton";
import { relativeTime } from "@/shared/lib/relative-time";

/** Publicly readable (Phase 30, ADR-0042) — `JoinOrganizationButton` self-guards on `!me`. */
export default function OrganizationDetailPage({ params }: PageProps<"/organizations/[id]">) {
  const { id } = use(params);
  const organizationId = Number(id);
  const { data: organization, isLoading, isError } = useOrganization(organizationId);

  if (isLoading) {
    return <Skeleton className="h-40 w-full" />;
  }

  if (isError || !organization) {
    return <p className="text-sm text-danger">조직을 찾을 수 없습니다.</p>;
  }

  return (
    <div className="space-y-4">
      <header className="space-y-2">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <div className="flex flex-wrap items-center gap-2">
            <h1 className="text-[32px] leading-10 font-bold tracking-tight">{organization.name}</h1>
            {organization.verified && (
              <span className="inline-flex h-[22px] items-center rounded bg-success-subtle px-2 text-xs font-semibold text-success">
                Verified · {organization.emailDomain}
              </span>
            )}
          </div>
          <JoinOrganizationButton organization={organization} />
        </div>
        <p className="text-[13px] text-text-secondary">
          멤버 {organization.memberCount}명 · {relativeTime(organization.createdAt)} 생성
        </p>
      </header>

      {organization.description && (
        <section className="rounded-[20px] border border-border bg-surface p-5">
          <p className="text-text-body">{organization.description}</p>
        </section>
      )}
    </div>
  );
}
