import type { StackDependency } from "../api/stack.types";

/** `io.r2dbc:r2dbc-pool` → `r2dbc-pool`; plugin and npm names stay as they are. */
export function shortName(dependency: StackDependency): string {
  return dependency.ecosystem === "MAVEN" ? (dependency.name.split(":").pop() ?? dependency.name) : dependency.name;
}

export function dependencyKey(dependency: StackDependency): string {
  return `${dependency.ecosystem}|${dependency.name}|${dependency.version ?? ""}`;
}
