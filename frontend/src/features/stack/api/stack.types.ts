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
