"use client";

import { use, useMemo, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useRequireAuth } from "@/features/auth/hooks/useRequireAuth";
import { useSession } from "@/features/auth/hooks/useSession";
import { useQuestion } from "@/features/question/hooks/useQuestion";
import { useAnswers } from "@/features/answer/hooks/useAnswers";
import { usePromoteQuestion, usePromotionCheck } from "@/features/question/hooks/usePromoteQuestion";
import { PromotionFindings } from "@/features/question/ui/PromotionFindings";
import { TagInput } from "@/features/question/ui/TagInput";
import type { PromotionDraft, PromotionField } from "@/features/question/api/question.types";
import { useDebouncedValue } from "@/shared/hooks/useDebouncedValue";
import { Input } from "@/shared/ui/Input";
import { Textarea } from "@/shared/ui/Textarea";
import { MarkdownEditor } from "@/shared/ui/MarkdownEditor";
import { Button } from "@/shared/ui/Button";
import { Skeleton } from "@/shared/ui/Skeleton";
import { FormError } from "@/shared/ui/FormError";

/**
 * "정제 후 공개" editor (Phase 79.3, ADR-0086). Starts from the private question and its accepted
 * answer; the author rewrites them while the server keeps pointing at what still looks internal.
 * Blocking findings (organization identifiers, secrets) disable publishing.
 */
export default function PromoteQuestionPage({ params }: PageProps<"/questions/[id]/promote">) {
  const { id } = use(params);
  const questionId = Number(id);
  const { isLoading: authLoading } = useRequireAuth();
  const { data: me } = useSession();
  const { data: question, isLoading } = useQuestion(questionId);
  const { data: answers, isLoading: answersLoading } = useAnswers(questionId);

  if (authLoading || isLoading || answersLoading) {
    return <Skeleton className="h-40 w-full" />;
  }

  const accepted = answers?.find((answer) => answer.isAccepted);
  const unavailable = !question
    ? "질문을 찾을 수 없습니다."
    : question.authorId !== me?.id
      ? "질문 작성자만 공개 카드로 정리할 수 있어요."
      : question.visibility !== "ORG"
        ? "이미 공개된 질문이에요."
        : question.promotedQuestionId
          ? "이미 공개 카드로 정리했어요."
          : question.status !== "RESOLVED" || !accepted
            ? "답변을 채택해 해결된 뒤에 정리할 수 있어요."
            : null;
  if (!question || !accepted || unavailable) {
    return (
      <div className="space-y-3 rounded-3xl border border-border bg-surface p-6">
        <p className="text-text-body">{unavailable}</p>
        <Link href={`/questions/${questionId}`} className="text-sm font-semibold text-brand hover:underline">
          질문으로 돌아가기
        </Link>
      </div>
    );
  }

  // Starts from the private question and its accepted answer; mounted once both are in, so later refetches never overwrite edits.
  return (
    <PromotionEditor
      questionId={questionId}
      initial={{
        title: question.title,
        body: question.body,
        environment: question.environment ?? "",
        logs: question.logs ?? "",
        tags: question.tags,
        solution: accepted.body,
      }}
    />
  );
}

function PromotionEditor({ questionId, initial }: { questionId: number; initial: PromotionDraft }) {
  const router = useRouter();
  const promote = usePromoteQuestion(questionId);
  const [draft, setDraft] = useState<PromotionDraft>(initial);

  const debounced = useDebouncedValue(draft, 500);
  const complete = debounced.title.trim() !== "" && debounced.body.trim() !== "" && debounced.solution.trim() !== "";
  const { data: findings = [], isFetching: checking } = usePromotionCheck(questionId, complete ? debounced : null);
  const blocking = findings.filter((finding) => finding.severity === "BLOCK").length;
  const countByField = useMemo(() => {
    const counts: Partial<Record<PromotionField, number>> = {};
    findings.forEach((finding) => (counts[finding.field] = (counts[finding.field] ?? 0) + 1));
    return counts;
  }, [findings]);
  const stale = draft !== debounced || checking;

  function update<K extends keyof PromotionDraft>(key: K, value: PromotionDraft[K]) {
    setDraft((current) => ({ ...current, [key]: value }));
  }

  async function handlePublish() {
    try {
      const result = await promote.mutateAsync({
        title: draft.title.trim(),
        body: draft.body.trim(),
        environment: draft.environment?.trim() || undefined,
        logs: draft.logs?.trim() || undefined,
        tags: draft.tags,
        solution: draft.solution.trim(),
      });
      router.push(`/questions/${result.id}`);
    } catch {
      // error surfaced below via promote.error
    }
  }

  function fieldNote(field: PromotionField) {
    const count = countByField[field];
    if (!count) return null;
    return <p className="text-xs font-semibold text-warning">이 칸에서 확인할 항목 {count}개</p>;
  }

  return (
    <div className="grid grid-cols-1 gap-10 lg:grid-cols-[minmax(0,1fr)_320px]">
      <div className="min-w-0 space-y-7">
        <div>
          <h1 className="text-[28px] leading-9 font-bold tracking-tight">공개 카드로 정리하기</h1>
          <p className="mt-1.5 text-text-body">
            새 공개 질문과, 해결 방법을 정리한 내 답변이 만들어집니다. 원래 질문과 그 판본·답변·댓글은 비공개로
            남고, 공개 카드에는 원래 질문으로 가는 링크가 붙지 않아요.
          </p>
        </div>

        <div className="space-y-7 rounded-3xl border border-border bg-surface px-5 py-6 sm:px-8 sm:py-7">
          <div className="space-y-2">
            <label htmlFor="promote-title" className="block font-semibold">
              제목
            </label>
            <Input
              id="promote-title"
              className="h-12 text-base"
              maxLength={300}
              value={draft.title}
              onChange={(event) => update("title", event.target.value)}
            />
            {fieldNote("title")}
          </div>

          <div className="space-y-2">
            <label htmlFor="promote-body" className="block font-semibold">
              본문
            </label>
            <MarkdownEditor id="promote-body" rows={10} value={draft.body} onChange={(value) => update("body", value)} />
            {fieldNote("body")}
          </div>

          <div className="space-y-2">
            <label htmlFor="promote-environment" className="block font-semibold">
              Environment (선택)
            </label>
            <Textarea
              id="promote-environment"
              rows={2}
              value={draft.environment}
              onChange={(event) => update("environment", event.target.value)}
            />
            {fieldNote("environment")}
          </div>

          <div className="space-y-2">
            <label htmlFor="promote-logs" className="block font-semibold">
              Logs (선택)
            </label>
            <Textarea
              id="promote-logs"
              rows={4}
              className="font-mono text-[13px]"
              value={draft.logs}
              onChange={(event) => update("logs", event.target.value)}
            />
            {fieldNote("logs")}
          </div>

          <div className="space-y-2">
            <span className="block font-semibold">태그</span>
            <TagInput value={draft.tags} onChange={(tags) => update("tags", tags)} />
            {fieldNote("tags")}
          </div>

          <div className="space-y-2">
            <label htmlFor="promote-solution" className="block font-semibold">
              해결 방법
            </label>
            <p className="text-sm text-text-secondary">
              채택된 답변에서 가져왔어요. 공개 카드에는 내 이름의 답변으로 올라가니, 내 말로 다듬어 주세요.
            </p>
            <MarkdownEditor
              id="promote-solution"
              rows={8}
              value={draft.solution}
              onChange={(value) => update("solution", value)}
            />
            {fieldNote("solution")}
          </div>

          <FormError error={promote.error} fallback="공개 카드를 만들지 못했습니다." />

          <div className="flex flex-wrap items-center justify-end gap-3 border-t border-border pt-4">
            <Link href={`/questions/${questionId}`} className="text-sm font-semibold text-text-secondary hover:text-text-primary">
              취소
            </Link>
            <Button
              type="button"
              className="h-12 px-7 text-[15px] font-bold"
              disabled={!complete || stale || blocking > 0 || promote.isPending}
              onClick={handlePublish}
            >
              {promote.isPending ? "만드는 중..." : "공개 카드 만들기"}
            </Button>
          </div>
        </div>
      </div>

      <aside>
        <PromotionFindings findings={findings} checking={stale} />
      </aside>
    </div>
  );
}
