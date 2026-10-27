/** Mirrors backend DependencyResponse / ParsedDependencyFileResponse (Phase 63, ADR-0076). */
export type Ecosystem = "MAVEN" | "GRADLE_PLUGIN" | "NPM";

export interface StackDependency {
  ecosystem: Ecosystem;
  name: string;
  /** As declared (`^19.2.0`, `4.0.8`); null for BOM-managed Maven coordinates. */
  version: string | null;
}

export interface ParsedDependencyFile {
  format: "GRADLE" | "MAVEN" | "NPM";
  dependencies: StackDependency[];
}

/** Where an answer holds for one package: `[fromVersion, untilVersion)` (Phase 63.3, ADR-0077). */
export interface ApplicabilityRange {
  ecosystem: Ecosystem;
  name: string;
  /** Inclusive lower bound. */
  fromVersion: string | null;
  /** Exclusive upper bound — where compatibility breaks, not the last version tried. */
  untilVersion: string | null;
}
