import Link from "next/link";
import type { Organization } from "@/entities/organization/model/organization.types";

export function OrganizationCard({ organization }: { organization: Organization }) {
  return (
    <li className="rounded-lg border border-border p-4">
      <div className="flex flex-wrap items-center gap-2">
        <Link href={`/organizations/${organization.id}`} className="font-medium hover:underline">
          {organization.name}
        </Link>
        {organization.verified && (
          <span className="inline-flex items-center rounded-full bg-success-subtle px-2 py-0.5 text-xs font-medium text-success">
            Verified · {organization.emailDomain}
          </span>
        )}
      </div>
      {organization.description && <p className="mt-1 text-sm text-text-secondary">{organization.description}</p>}
      <p className="mt-2 text-xs text-text-secondary">멤버 {organization.memberCount}명</p>
    </li>
  );
}
