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
    <ul className="divide-y divide-border overflow-hidden rounded-xl border border-border bg-surface">
      {cards.map((card, index) => {
        const content = (
          <>
            <span className="mr-2 inline-flex h-[22px] items-center rounded border border-border-strong px-2 font-mono text-[11px] text-text-body">
              {typeLabels[card.type]}
            </span>
            {card.headline}
          </>
        );
        return (
          <li key={index} className="px-4 py-3 text-sm text-text-primary">
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
