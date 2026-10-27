"use client";

import { useState } from "react";
import { useCreateAnswer } from "../hooks/useCreateAnswer";
import { MarkdownEditor } from "@/shared/ui/MarkdownEditor";
import { Button } from "@/shared/ui/Button";
import { FormError } from "@/shared/ui/FormError";
import type { StackDependency } from "@/features/stack/api/stack.types";
import { type ApplicabilityRow, rowsToRanges } from "@/features/stack/lib/applicability";
import { ApplicabilityRangeEditor } from "@/features/stack/ui/ApplicabilityRangeEditor";

export function AnswerComposer({
  questionId,
  questionDependencies = [],
}: {
  questionId: number;
  /** The question's stack fingerprint, offered as applicability rows (ADR-0077). */
  questionDependencies?: StackDependency[];
}) {
  const [body, setBody] = useState("");
  const [rows, setRows] = useState<ApplicabilityRow[]>([]);
  const createAnswer = useCreateAnswer(questionId);

  async function handleSubmit() {
    if (!body.trim()) return;
    try {
      await createAnswer.mutateAsync({ body: body.trim(), applicability: rowsToRanges(rows) });
      setBody("");
      setRows([]);
    } catch {
      // error surfaced below via createAnswer.error
    }
  }

  return (
    <div className="space-y-3">
      <h2 className="text-lg font-semibold">Your Answer</h2>
      <MarkdownEditor value={body} onChange={setBody} rows={8} placeholder="답변을 작성하세요 (Markdown 지원)" />
      <ApplicabilityRangeEditor rows={rows} onChange={setRows} suggestions={questionDependencies} idPrefix="compose" />
      <FormError error={createAnswer.error} fallback="답변을 등록하지 못했습니다." />
      <Button onClick={handleSubmit} disabled={createAnswer.isPending || !body.trim()}>
        {createAnswer.isPending ? "등록 중..." : "Post Answer"}
      </Button>
    </div>
  );
}
