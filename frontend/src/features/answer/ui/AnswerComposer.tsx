"use client";

import { useState } from "react";
import { useCreateAnswer } from "../hooks/useCreateAnswer";
import { MarkdownEditor } from "@/shared/ui/MarkdownEditor";
import { Button } from "@/shared/ui/Button";
import { FormError } from "@/shared/ui/FormError";

export function AnswerComposer({ questionId }: { questionId: number }) {
  const [body, setBody] = useState("");
  const createAnswer = useCreateAnswer(questionId);

  async function handleSubmit() {
    if (!body.trim()) return;
    try {
      await createAnswer.mutateAsync(body.trim());
      setBody("");
    } catch {
      // error surfaced below via createAnswer.error
    }
  }

  return (
    <div className="space-y-3">
      <h2 className="text-lg font-semibold">Your Answer</h2>
      <MarkdownEditor value={body} onChange={setBody} rows={8} placeholder="답변을 작성하세요 (Markdown 지원)" />
      <FormError error={createAnswer.error} fallback="답변을 등록하지 못했습니다." />
      <Button onClick={handleSubmit} disabled={createAnswer.isPending || !body.trim()}>
        {createAnswer.isPending ? "등록 중..." : "Post Answer"}
      </Button>
    </div>
  );
}
