"use client";

import type { Ecosystem, StackDependency } from "../api/stack.types";
import { type ApplicabilityRow, rowFromDependency } from "../lib/applicability";
import { shortName } from "../lib/dependency-label";
import { Input } from "@/shared/ui/Input";

const ECOSYSTEMS: { value: Ecosystem; label: string }[] = [
  { value: "MAVEN", label: "Maven" },
  { value: "GRADLE_PLUGIN", label: "Gradle 플러그인" },
  { value: "NPM", label: "npm" },
];

const MAX_ROWS = 20;

/**
 * Where an answer holds, per package: "이상" (inclusive) and "미만" (exclusive) — Phase 63.3,
 * ADR-0077. The question's stack fingerprint is offered as one-click rows starting at the asker's
 * version; any other package can be typed in.
 */
export function ApplicabilityRangeEditor({
  rows,
  onChange,
  suggestions = [],
  idPrefix,
}: {
  rows: ApplicabilityRow[];
  onChange: (rows: ApplicabilityRow[]) => void;
  /** The question's dependencies. */
  suggestions?: StackDependency[];
  /** Keeps input ids unique when several editors are on one page. */
  idPrefix: string;
}) {
  const used = new Set(rows.map((row) => `${row.ecosystem}|${row.name}`));
  const offered = suggestions.filter((dependency) => !used.has(`${dependency.ecosystem}|${dependency.name}`));
  const full = rows.length >= MAX_ROWS;

  function update(index: number, patch: Partial<ApplicabilityRow>) {
    onChange(rows.map((row, i) => (i === index ? { ...row, ...patch } : row)));
  }

  return (
    <fieldset className="space-y-3 rounded-2xl border border-border p-4">
      <legend className="px-1 text-sm font-semibold">적용 범위 (선택)</legend>
      <p className="text-xs text-text-secondary">
        이 답변이 유효한 버전을 적어 두면, 범위 안의 새 릴리스로는 재검토 알림이 가지 않아요. 상한은 호환이 깨지는
        버전(미만)으로 적어 주세요.
      </p>

      {offered.length > 0 && !full && (
        <div className="flex flex-wrap items-center gap-1.5">
          <span className="text-xs text-text-secondary">질문 환경에서 추가</span>
          {offered.slice(0, 8).map((dependency) => (
            <button
              key={`${dependency.ecosystem}|${dependency.name}`}
              type="button"
              onClick={() => onChange([...rows, rowFromDependency(dependency)])}
              className="inline-flex h-[26px] items-center rounded-full border border-border px-2.5 font-mono text-xs text-text-body hover:bg-surface-subtle"
            >
              + {shortName(dependency)}
            </button>
          ))}
        </div>
      )}

      {rows.length > 0 && (
        <ul className="space-y-2">
          {rows.map((row, index) => {
            const id = `${idPrefix}-range-${index}`;
            return (
              <li key={index} className="flex flex-wrap items-center gap-2">
                <label className="sr-only" htmlFor={`${id}-ecosystem`}>
                  생태계
                </label>
                <select
                  id={`${id}-ecosystem`}
                  value={row.ecosystem}
                  onChange={(event) => update(index, { ecosystem: event.target.value as Ecosystem })}
                  className="h-11 w-36 rounded-full border border-border-strong bg-surface px-3 text-sm text-text-primary"
                >
                  {ECOSYSTEMS.map((option) => (
                    <option key={option.value} value={option.value}>
                      {option.label}
                    </option>
                  ))}
                </select>
                <label className="sr-only" htmlFor={`${id}-name`}>
                  패키지
                </label>
                <div className="min-w-56 flex-1">
                  <Input
                    id={`${id}-name`}
                    value={row.name}
                    onChange={(event) => update(index, { name: event.target.value })}
                    placeholder="group:artifact 또는 패키지명"
                    className="font-mono text-[13px]"
                  />
                </div>
                <label className="sr-only" htmlFor={`${id}-from`}>
                  이상
                </label>
                <div className="w-28">
                  <Input
                    id={`${id}-from`}
                    value={row.fromVersion}
                    onChange={(event) => update(index, { fromVersion: event.target.value })}
                    placeholder="이상"
                    className="font-mono text-[13px]"
                  />
                </div>
                <label className="sr-only" htmlFor={`${id}-until`}>
                  미만
                </label>
                <div className="w-28">
                  <Input
                    id={`${id}-until`}
                    value={row.untilVersion}
                    onChange={(event) => update(index, { untilVersion: event.target.value })}
                    placeholder="미만"
                    className="font-mono text-[13px]"
                  />
                </div>
                <button
                  type="button"
                  onClick={() => onChange(rows.filter((_, i) => i !== index))}
                  aria-label={`${row.name || "빈 항목"} 범위 삭제`}
                  className="h-11 rounded-full px-3 text-sm text-text-secondary hover:bg-surface-subtle hover:text-text-primary"
                >
                  삭제
                </button>
              </li>
            );
          })}
        </ul>
      )}

      {!full && (
        <button
          type="button"
          onClick={() => onChange([...rows, { ecosystem: "MAVEN", name: "", fromVersion: "", untilVersion: "" }])}
          className="text-sm font-semibold text-brand hover:underline"
        >
          + 패키지 직접 추가
        </button>
      )}
    </fieldset>
  );
}
