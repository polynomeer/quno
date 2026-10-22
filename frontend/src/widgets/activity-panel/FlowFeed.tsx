import Link from "next/link";
import type { FlowCard, FlowCardType } from "@/features/dashboard/api/dashboard.types";

/** Fixed section order matches the backend (popular → tag spike → reopened → super answer) —
 * see docs/architecture/api-design.md #quno-flow-고급-dashboard-phase-10. */
const typeLabels: Record<FlowCardType, string> = {
  POPULAR_QUESTION: "인기 질문",
  TAG_SPIKE: "태그 급증",
  REOPENED_QUESTION: "재활성화",
  CLUSTER_SUPER_ANSWER: "Super Answer",
};

export function FlowFeed({ cards }: { cards: FlowCard[] }) {
  if (cards.length === 0) {
    return <p className="text-sm text-text-secondary">아직 활동이 없습니다.</p>;
  }

  return (
    <ul className="divide-y divide-border overflow-hidden rounded-[20px] border border-border bg-surface">
      {cards.map((card, index) => {
        const content = (
          <>
            <span className="mr-2 inline-flex h-6 items-center rounded-full bg-surface-subtle px-2.5 text-xs font-semibold text-text-body">
              {typeLabels[card.type]}
            </span>
            {card.headline}
          </>
        );
        return (
          <li key={index} className="px-5 py-3.5 text-sm text-text-primary">
            {card.questionId ? (
              <Link href={`/questions/${card.questionId}`} className="hover:text-brand">
                {content}
              </Link>
            ) : (
              content
            )}
          </li>
        );
      })}
    </ul>
  );
}
