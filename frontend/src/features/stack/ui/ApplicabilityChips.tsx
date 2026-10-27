import type { ApplicabilityRange } from "../api/stack.types";
import { rangeLabel, rangeName } from "../lib/applicability";

/** An answer's applicability ranges (Phase 63.3, ADR-0077); nothing for answers without any. */
export function ApplicabilityChips({ ranges }: { ranges: ApplicabilityRange[] }) {
  if (ranges.length === 0) return null;

  return (
    <ul aria-label="적용 범위" className="flex flex-wrap gap-1.5">
      {ranges.map((range) => (
        <li
          key={`${range.ecosystem}|${range.name}`}
          title={range.name}
          className="inline-flex h-[26px] items-center gap-1.5 rounded-full bg-brand-subtle px-2.5 text-xs text-brand-on-subtle"
        >
          <span className="font-mono font-semibold">{rangeName(range)}</span>
          <span>{rangeLabel(range)}</span>
        </li>
      ))}
    </ul>
  );
}
