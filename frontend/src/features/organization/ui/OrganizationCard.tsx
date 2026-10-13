import Link from "next/link";
import type { Organization } from "@/entities/organization/model/organization.types";

export function OrganizationCard({ organization }: { organization: Organization }) {
  return (
    <li className="px-4 py-4 transition-colors hover:bg-canvas/60 sm:px-5">
      <div className="flex flex-wrap items-center gap-2">
        <Link href={`/organizations/${organization.id}`} className="font-medium text-text-primary hover:text-brand">
          {organization.name}
        </Link>
        {organization.verified && (
          <span className="inline-flex h-[22px] items-center rounded bg-success-subtle px-2 text-xs font-semibold text-success">
            Verified · {organization.emailDomain}
          </span>
        )}
      </div>
      {organization.description && <p className="mt-1 text-sm text-text-body">{organization.description}</p>}
      <p className="mt-2 text-[13px] text-text-secondary">멤버 {organization.memberCount}명</p>
    </li>
  );
}
