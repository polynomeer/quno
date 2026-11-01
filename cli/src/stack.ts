import { existsSync, readFileSync } from "node:fs";
import { join } from "node:path";
import type { QunoClient } from "./client.js";
import type { StackDependency } from "./types.js";

/** Looked for in this order; the first one found is the project's build file. */
export const DEPENDENCY_FILES = ["build.gradle.kts", "build.gradle", "pom.xml", "package.json"] as const;

export function findDependencyFile(dir: string): string | null {
  return DEPENDENCY_FILES.find((name) => existsSync(join(dir, name))) ?? null;
}

/** `com.acme:*` matches by prefix, anything else exactly (ADR-0080's stand-in for the web checkboxes). */
export function isExcluded(dependency: StackDependency, patterns: string[]): boolean {
  return patterns.some((pattern) =>
    pattern.endsWith("*") ? dependency.name.startsWith(pattern.slice(0, -1)) : dependency.name === pattern,
  );
}

/** Parses the build file on the server (the file itself is never stored, ADR-0076) and drops excluded entries. */
export async function detectStack(
  client: QunoClient,
  dir: string,
  exclude: string[],
): Promise<{ file: string; dependencies: StackDependency[]; excluded: number } | null> {
  const file = findDependencyFile(dir);
  if (!file) return null;
  const parsed = await client.parseDependencies(file, readFileSync(join(dir, file), "utf8").slice(0, 100_000));
  const kept = parsed.dependencies.filter((dependency) => !isExcluded(dependency, exclude));
  return { file, dependencies: kept, excluded: parsed.dependencies.length - kept.length };
}

export function dependencyLabel(dependency: StackDependency): string {
  return dependency.version ? `${dependency.name} ${dependency.version}` : dependency.name;
}
