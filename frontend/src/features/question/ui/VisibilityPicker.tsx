import Link from "next/link";
import type { Organization } from "@/entities/organization/model/organization.types";
import { useLocale } from "@/shared/i18n/LocaleProvider";
import { cn } from "@/shared/lib/cn";

interface VisibilityPickerProps {
  /** The asker's organizations; only verified ones can own a private question (ADR-0085). */
  organizations: Organization[];
  /** null = public. */
  value: number | null;
  onChange: (organizationId: number | null) => void;
}

/**
 * "공개 범위" on the ask form (Phase 79). Community organizations are left out on purpose: anyone
 * can join one, so "members only" would promise more than it keeps.
 */
export function VisibilityPicker({ organizations, value, onChange }: VisibilityPickerProps) {
  const { t } = useLocale();
  const verified = organizations.filter((organization) => organization.verified);
  const options = [
    { id: null, label: t.ask.visibilityPublic, hint: t.ask.visibilityPublicHint },
    ...verified.map((organization) => ({
      id: organization.id,
      label: t.ask.visibilityOrg.replace("{name}", organization.name),
      hint: t.ask.visibilityOrgHint,
    })),
  ];

  return (
    <fieldset className="space-y-2">
      <legend className="mb-2 font-semibold">{t.ask.visibilityLabel}</legend>
      <div className="grid gap-2 sm:grid-cols-2">
        {options.map((option) => {
          const checked = option.id === value;
          return (
            <label
              key={option.id ?? "public"}
              className={cn(
                "flex cursor-pointer gap-2.5 rounded-xl border px-3.5 py-3 text-sm",
                checked ? "border-brand bg-brand-subtle" : "border-border",
              )}
            >
              <input
                type="radio"
                name="ask-visibility"
                className="mt-0.5"
                checked={checked}
                onChange={() => onChange(option.id)}
              />
              <span className="min-w-0">
                <span className="block font-semibold">
                  {option.id !== null && <span aria-hidden="true">🔒 </span>}
                  {option.label}
                </span>
                <span className="block text-xs text-text-secondary">{option.hint}</span>
              </span>
            </label>
          );
        })}
      </div>
      {verified.length === 0 && (
        <p className="text-xs text-text-secondary">
          {t.ask.visibilityNoVerifiedOrg}{" "}
          <Link href="/organizations" className="font-semibold text-brand underline-offset-2 hover:underline">
            {t.ask.visibilityOrganizationsLink}
          </Link>
        </p>
      )}
    </fieldset>
  );
}
