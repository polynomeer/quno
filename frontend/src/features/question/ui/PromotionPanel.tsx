import Link from "next/link";
import type { QuestionDetail } from "../api/question.types";

/**
 * "정제 후 공개" on a private question (Phase 79.3, ADR-0086). Once it is resolved, its author is
 * offered a sanitized public card; after that, members see where the card is. Nothing for
 * public questions or before an answer is accepted.
 */
export function PromotionPanel({ question, isAuthor }: { question: QuestionDetail; isAuthor: boolean }) {
  if (question.visibility !== "ORG") return null;

  if (question.promotedQuestionId) {
    return (
      <section className="flex flex-wrap items-center justify-between gap-3 rounded-[20px] border border-border bg-surface px-5 py-4">
        <p className="text-sm text-text-body">사내 정보를 지운 공개 카드로 정리됐어요.</p>
        <Link
          href={`/questions/${question.promotedQuestionId}`}
          className="text-sm font-semibold text-brand hover:underline"
        >
          공개 카드 보기
        </Link>
      </section>
    );
  }

  if (!isAuthor || question.status !== "RESOLVED") return null;

  return (
    <section className="space-y-3 rounded-[20px] bg-brand-subtle px-5 py-4">
      <div className="space-y-1">
        <h2 className="text-sm font-bold text-brand-on-subtle">해결됐어요. 공개 카드로 남겨 볼까요?</h2>
        <p className="text-sm text-text-body">
          회사 이름·내부 주소·비밀값을 지우고 일반화한 새 공개 질문과 해결 방법을 만듭니다. 이 질문과 지금까지의
          판본·답변·댓글은 그대로 비공개로 남아요.
        </p>
      </div>
      <Link
        href={`/questions/${question.id}/promote`}
        className="inline-flex h-9 items-center rounded-full bg-brand px-4 text-sm font-semibold text-brand-foreground hover:opacity-90"
      >
        정제해서 공개하기
      </Link>
    </section>
  );
}
