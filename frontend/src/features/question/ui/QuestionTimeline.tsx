"use client";

import Link from "next/link";
import { relativeTime } from "@/shared/lib/relative-time";
import { cn } from "@/shared/lib/cn";
import { useQuestionTimeline } from "../hooks/useQuestionTimeline";
import type { QuestionTimelineEvent } from "../api/question.types";

function describe(event: QuestionTimelineEvent): string {
  switch (event.type) {
    case "QUESTION_CREATED":
      return "질문 등록 · rev 1";
    case "QUESTION_REVISED":
      return `rev ${event.versionNumber} 리비전`;
    case "ANSWER_POSTED":
      return event.accepted ? "답변 · 수락됨" : "답변";
    case "REVIEW_REQUESTED":
      return "QPR 정보 요청";
    case "REVIEW_ADDRESSED":
      return "QPR 요청 반영됨";
  }
}

// Filled dots mark the events that changed the question's state the most; hollow ones are context.
// Each tone pairs with text (the label), so state never rides on color alone (design.md #23).
function dotClass(event: QuestionTimelineEvent): string {
  switch (event.type) {
    case "QUESTION_CREATED":
      return "bg-text-primary";
    case "QUESTION_REVISED":
      return "bg-brand";
    case "ANSWER_POSTED":
      return event.accepted ? "bg-success-strong" : "border-2 border-text-secondary";
    case "REVIEW_REQUESTED":
      return "border-2 border-danger";
    case "REVIEW_ADDRESSED":
      return "border-2 border-brand";
  }
}

/** "질문의 생애" (design.md #3.3, ADR-0062) — only events that changed what the question means. */
export function QuestionTimeline({ questionId }: { questionId: number }) {
  const { data: events } = useQuestionTimeline(questionId);

  if (!events || events.length === 0) {
    return null;
  }

  return (
    <section className="rounded-xl border border-border bg-surface p-5">
      <h2 className="mb-4 text-sm font-semibold text-text-primary">질문의 생애</h2>
      <ol>
        {events.map((event, index) => {
          const isLast = index === events.length - 1;
          const label = describe(event);
          return (
            <li key={`${event.type}-${event.occurredAt}-${event.answerId ?? ""}`} className="flex gap-3">
              <span aria-hidden="true" className="flex flex-col items-center">
                <span className={cn("mt-1.5 size-2.5 shrink-0 rounded-full", dotClass(event))} />
                {!isLast && <span className="mt-1 w-0.5 flex-1 bg-border" />}
              </span>
              <span className={cn("flex min-w-0 flex-col leading-snug", !isLast && "pb-4")}>
                {event.answerId ? (
                  <a href={`#answer-${event.answerId}`} className="text-[13px] font-medium text-text-primary hover:text-brand">
                    {label}
                  </a>
                ) : (
                  <span className="text-[13px] font-medium text-text-primary">{label}</span>
                )}
                <span className="font-mono text-[11px] text-text-secondary">
                  {event.actorId !== null && `사용자 #${event.actorId} · `}
                  {relativeTime(event.occurredAt)}
                </span>
              </span>
            </li>
          );
        })}
      </ol>
      {events.some((event) => event.type === "QUESTION_REVISED") && (
        <Link
          href={`/questions/${questionId}/versions`}
          className="mt-2 inline-block text-[13px] font-semibold text-brand hover:underline"
        >
          리비전 비교 (diff) →
        </Link>
      )}
    </section>
  );
}
