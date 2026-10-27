import type { StackDependency } from "../api/stack.types";
import { dependencyKey, shortName } from "../lib/dependency-label";

const VISIBLE = 8;

/** The question's stack fingerprint as compact chips (Phase 63, ADR-0076); the full coordinate is
 * in the title attribute. Shows nothing for questions without one. */
export function DependencyChips({ dependencies }: { dependencies: StackDependency[] }) {
  if (dependencies.length === 0) return null;
  const hidden = dependencies.length - VISIBLE;

  return (
    <ul aria-label="스택 지문" className="flex flex-wrap gap-1.5">
      {dependencies.slice(0, VISIBLE).map((dependency) => (
        <li
          key={dependencyKey(dependency)}
          title={`${dependency.name}${dependency.version ? ` ${dependency.version}` : ""}`}
          className="inline-flex h-[26px] items-center gap-1.5 rounded-full border border-border px-2.5 font-mono text-xs text-text-body"
        >
          {shortName(dependency)}
          {dependency.version && <span className="text-text-secondary">{dependency.version}</span>}
        </li>
      ))}
      {hidden > 0 && (
        <li className="inline-flex h-[26px] items-center rounded-full px-2 text-xs text-text-secondary">+{hidden}</li>
      )}
    </ul>
  );
}
