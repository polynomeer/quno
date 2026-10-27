"use client";

import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { stackApi } from "../api/stack.api";
import type { StackDependency } from "../api/stack.types";
import { dependencyKey } from "../lib/dependency-label";
import { Button } from "@/shared/ui/Button";
import { FormError } from "@/shared/ui/FormError";
import { Textarea } from "@/shared/ui/Textarea";
import { useLocale } from "@/shared/i18n/LocaleProvider";

const FILE_OPTIONS = ["build.gradle.kts", "build.gradle", "pom.xml", "package.json"] as const;

/**
 * Paste a build file → the server parses it → the author unticks anything they'd rather not
 * share (in-house packages) → [onChange] reports only the kept entries (Phase 63, ADR-0076).
 * The pasted text stays in this component; it is never part of the question.
 */
export function StackFingerprintInput({
  onChange,
  hint,
}: {
  onChange: (dependencies: StackDependency[]) => void;
  /** Overrides the ask form's wording, e.g. on "내 스택" where nothing goes with a question. */
  hint?: string;
}) {
  const { t } = useLocale();
  const [fileName, setFileName] = useState("");
  const [content, setContent] = useState("");
  const [candidates, setCandidates] = useState<StackDependency[] | null>(null);
  const [excluded, setExcluded] = useState<Set<string>>(new Set());
  const parse = useMutation({ mutationFn: () => stackApi.parse(content, fileName || undefined) });

  function report(list: StackDependency[], skip: Set<string>) {
    onChange(list.filter((dependency) => !skip.has(dependencyKey(dependency))));
  }

  function read() {
    parse.mutate(undefined, {
      onSuccess: (result) => {
        setCandidates(result.dependencies);
        setExcluded(new Set());
        report(result.dependencies, new Set());
      },
    });
  }

  function toggle(dependency: StackDependency) {
    const key = dependencyKey(dependency);
    const next = new Set(excluded);
    if (next.has(key)) next.delete(key);
    else next.add(key);
    setExcluded(next);
    report(candidates ?? [], next);
  }

  const kept = (candidates?.length ?? 0) - excluded.size;

  return (
    <div className="space-y-3">
      <p className="text-sm text-text-secondary">{hint ?? t.ask.stackHint}</p>
      <div className="flex flex-wrap items-end gap-3">
        <label className="flex flex-col gap-1 text-sm font-medium">
          {t.ask.stackFileLabel}
          <select
            value={fileName}
            onChange={(event) => setFileName(event.target.value)}
            className="h-11 rounded-full border border-border-strong bg-surface px-4 text-sm text-text-primary"
          >
            <option value="">{t.ask.stackFileAuto}</option>
            {FILE_OPTIONS.map((option) => (
              <option key={option} value={option}>
                {option}
              </option>
            ))}
          </select>
        </label>
        <Button type="button" variant="secondary" onClick={read} disabled={!content.trim() || parse.isPending}>
          {parse.isPending ? t.ask.stackParsing : t.ask.stackParse}
        </Button>
      </div>
      <label htmlFor="ask-stack-content" className="sr-only">
        {t.ask.stackContentLabel}
      </label>
      <Textarea
        id="ask-stack-content"
        rows={4}
        value={content}
        onChange={(event) => setContent(event.target.value)}
        className="font-mono text-[13px]"
      />
      <FormError error={parse.error} fallback={t.ask.stackParseFailed} />

      {candidates !== null &&
        (candidates.length === 0 ? (
          <p className="text-sm text-text-secondary">{t.ask.stackNone}</p>
        ) : (
          <fieldset className="overflow-hidden rounded-2xl border border-border">
            <legend className="sr-only">{t.ask.stackLabel}</legend>
            <p className="bg-surface-subtle px-4 py-2 text-xs font-semibold text-text-body">
              {t.ask.stackSelected.replace("{kept}", String(kept)).replace("{total}", String(candidates.length))}
            </p>
            <ul className="max-h-72 divide-y divide-border overflow-y-auto">
              {candidates.map((dependency) => {
                const key = dependencyKey(dependency);
                const checked = !excluded.has(key);
                return (
                  <li key={key}>
                    <label className="flex min-h-11 cursor-pointer items-center gap-3 px-4 py-2 text-sm">
                      <input
                        type="checkbox"
                        checked={checked}
                        onChange={() => toggle(dependency)}
                        className="size-[18px] shrink-0 accent-brand"
                      />
                      <span className={checked ? "min-w-0 flex-1 font-mono text-[13px]" : "min-w-0 flex-1 font-mono text-[13px] text-text-secondary line-through"}>
                        {dependency.name}
                      </span>
                      <span className="font-mono text-xs text-text-secondary">{dependency.version ?? "—"}</span>
                    </label>
                  </li>
                );
              })}
            </ul>
          </fieldset>
        ))}
    </div>
  );
}
