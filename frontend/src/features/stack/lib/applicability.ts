import type { ApplicabilityRange, StackDependency } from "../api/stack.types";
import { shortName } from "./dependency-label";

/** One editor row; empty strings mean "not given yet". */
export interface ApplicabilityRow {
  ecosystem: ApplicabilityRange["ecosystem"];
  name: string;
  fromVersion: string;
  untilVersion: string;
}

/** `3.2.0 이상 · 4.0.0 미만` — the half-open range in words (ADR-0077). */
export function rangeLabel(range: ApplicabilityRange): string {
  const parts = [];
  if (range.fromVersion) parts.push(`${range.fromVersion} 이상`);
  if (range.untilVersion) parts.push(`${range.untilVersion} 미만`);
  return parts.join(" · ");
}

export function rangeName(range: ApplicabilityRange): string {
  return shortName({ ecosystem: range.ecosystem, name: range.name, version: null });
}

/** `^3.2.0` → `3.2.0`: a declared npm range read as its lower bound, the same way the backend does. */
export function declaredLowerBound(version: string | null): string {
  return (version ?? "").replace(/^[\^~>=<\s]+/, "");
}

/** A row pre-filled from one of the question's dependencies — "holds from the version you're on". */
export function rowFromDependency(dependency: StackDependency): ApplicabilityRow {
  return { ecosystem: dependency.ecosystem, name: dependency.name, fromVersion: declaredLowerBound(dependency.version), untilVersion: "" };
}

export function rowFromRange(range: ApplicabilityRange): ApplicabilityRow {
  return { ecosystem: range.ecosystem, name: range.name, fromVersion: range.fromVersion ?? "", untilVersion: range.untilVersion ?? "" };
}

/** Rows without a package name are dropped; blank bounds become null. Version checks stay on the server. */
export function rowsToRanges(rows: ApplicabilityRow[]): ApplicabilityRange[] {
  return rows
    .filter((row) => row.name.trim())
    .map((row) => ({
      ecosystem: row.ecosystem,
      name: row.name.trim(),
      fromVersion: row.fromVersion.trim() || null,
      untilVersion: row.untilVersion.trim() || null,
    }));
}

export function sameRanges(left: ApplicabilityRange[], right: ApplicabilityRange[]): boolean {
  return JSON.stringify(left) === JSON.stringify(right);
}
