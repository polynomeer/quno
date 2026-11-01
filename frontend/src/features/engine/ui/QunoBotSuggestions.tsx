"use client";

import type { EngineField, EngineSuggestion, EngineSuggestionKind } from "../api/engine.api";
import { lineDiff } from "../lib/line-diff";
import { useEngineSuggestions, useResolveEngineSuggestion } from "../hooks/useEngineSuggestions";
import { QunoBotIcon } from "./QunoBotIcon";
import { Button } from "@/shared/ui/Button";
import { FormError } from "@/shared/ui/FormError";
import { cn } from "@/shared/lib/cn";

const FIELD_LABEL: Record<EngineField, string> = { title: "제목", body: "본문", environment: "환경", logs: "로그" };

/** Why QunoBot thinks so, and what "무시" means, per kind (ADR-0081, ADR-0082). */
const KIND_COPY: Record<EngineSuggestionKind, { basis: string; dismiss: string }> = {
  SECRET_REDACTION: { basis: "패턴 + 엔트로피", dismiss: "무시 — 비밀값이 아니에요" },
  ADD_CODE_FENCE_LANGUAGE: { basis: "코드 모양", dismiss: "무시 — 이 질문에서 다시 제안하지 않아요" },
  FENCE_STACK_TRACE: { basis: "스택 프레임 3줄 이상", dismiss: "무시 — 이 질문에서 다시 제안하지 않아요" },
  MOVE_LONG_LOG: { basis: "30줄 이상의 로그", dismiss: "무시 — 이 질문에서 다시 제안하지 않아요" },
};

export interface CurrentContent {
  title: string;
  body: string;
  environment: string | null;
  logs: string | null;
}

function proposedText(suggestion: EngineSuggestion, field: EngineField): string | null {
  return {
    title: suggestion.proposedTitle,
    body: suggestion.proposedBody,
    environment: suggestion.proposedEnvironment,
    logs: suggestion.proposedLogs,
  }[field];
}

/**
 * QunoBot suggestions on the author's own question (Phase 78, the "QunoBot 제안" card of web
 * design v2). The engine never edits: accepting makes a revision under the author's name,
 * dismissing a redaction says "this is not a secret" and unmasks it for everyone (ADR-0081).
 */
export function QunoBotSuggestions({ questionId, current }: { questionId: number; current: CurrentContent }) {
  const { data } = useEngineSuggestions(questionId, true);
  const resolve = useResolveEngineSuggestion(questionId);
  const open = (data ?? []).filter((suggestion) => suggestion.status === "OPEN");

  if (open.length === 0) return null;

  return (
    <section
      id="qunobot"
      aria-labelledby="qunobot-heading"
      className="scroll-mt-28 space-y-3 rounded-[22px] border-[1.5px] border-dashed border-brand/50 bg-surface px-5 py-4 sm:px-[22px] sm:py-[18px]"
    >
      <div className="flex flex-wrap items-center gap-2.5">
        <span className="grid size-8 place-items-center rounded-[10px] bg-brand-subtle text-brand-on-subtle">
          <QunoBotIcon className="size-[18px]" />
        </span>
        <h2 id="qunobot-heading" className="text-[15px] font-bold">
          QunoBot 제안 {open.length}건
        </h2>
        <span className="text-xs text-text-secondary">엔진은 원본을 바꾸지 않아요. 수락하면 작성자 이름으로 새 리비전이 돼요.</span>
      </div>

      <FormError error={resolve.error} fallback="제안을 처리하지 못했습니다." />

      <ul className="space-y-2">
        {open.map((suggestion) => {
          const busy = resolve.isPending && resolve.variables?.id === suggestion.id;
          return (
            <li key={suggestion.id} className="space-y-2 rounded-[14px] bg-surface-subtle px-3.5 py-3">
              <div className="flex flex-wrap items-center gap-2.5">
                <p className="min-w-0 flex-[1_1_320px] text-sm">
                  {suggestion.summary}{" "}
                  <span className="text-text-secondary">· 근거: {KIND_COPY[suggestion.kind].basis}</span>
                </p>
                <Button onClick={() => resolve.mutate({ id: suggestion.id, action: "accept" })} disabled={resolve.isPending}>
                  {busy && resolve.variables?.action === "accept" ? "반영 중..." : "수락"}
                </Button>
                <Button
                  variant="ghost"
                  onClick={() => resolve.mutate({ id: suggestion.id, action: "dismiss" })}
                  disabled={resolve.isPending}
                  aria-label={KIND_COPY[suggestion.kind].dismiss}
                >
                  무시
                </Button>
              </div>
              {suggestion.kind === "SECRET_REDACTION" && (
                <p className="text-xs text-text-secondary">
                  이미 공개된 적이 있는 키·토큰이에요. 지우는 것과 별개로 해당 서비스에서 폐기하고 새로 발급해 주세요. 무시하면 비밀값이 아닌 것으로 보고 다시 보여요.
                </p>
              )}
              <details className="text-xs">
                <summary className="cursor-pointer font-semibold text-text-body">
                  바뀔 내용 보기 ({suggestion.changedFields.map((field) => FIELD_LABEL[field]).join(", ")})
                </summary>
                {suggestion.changedFields.map((field) => (
                  <div key={field} className="mt-2">
                    <p className="font-medium text-text-secondary">{FIELD_LABEL[field]}</p>
                    <pre
                      aria-label={`${FIELD_LABEL[field]} 변경`}
                      className="mt-1 max-h-64 overflow-auto rounded-xl bg-code-bg p-3 font-mono text-code-fg whitespace-pre-wrap"
                    >
                      {lineDiff(current[field] ?? "", proposedText(suggestion, field) ?? "").map((row, index) => (
                        <span
                          key={index}
                          className={cn(
                            "block",
                            row.type === "added" && "bg-success/20",
                            row.type === "removed" && "bg-danger/20 line-through opacity-80",
                            row.type === "gap" && "opacity-60",
                          )}
                        >
                          {row.type === "added" ? "+ " : row.type === "removed" ? "- " : "  "}
                          {row.text}
                        </span>
                      ))}
                    </pre>
                  </div>
                ))}
                <p className="mt-2 font-mono text-[11px] text-text-secondary">
                  {suggestion.engine}@{suggestion.engineVersion} · v{suggestion.targetVersionNumber} · 입력 {suggestion.inputHash.slice(0, 12)}
                </p>
              </details>
            </li>
          );
        })}
      </ul>
    </section>
  );
}
