"use client";

import { useState } from "react";
import { useCreateAnswer } from "../hooks/useCreateAnswer";
import { MarkdownEditor } from "@/shared/ui/MarkdownEditor";
import { Button } from "@/shared/ui/Button";
import { ApiError } from "@/shared/api/api-error";

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
      {createAnswer.isError && (
        <p className="text-sm text-danger">
          {createAnswer.error instanceof ApiError ? createAnswer.error.message : "답변을 등록하지 못했습니다."}
        </p>
      )}
      <Button onClick={handleSubmit} disabled={createAnswer.isPending || !body.trim()}>
        {createAnswer.isPending ? "등록 중..." : "Post Answer"}
      </Button>
    </div>
  );
}
