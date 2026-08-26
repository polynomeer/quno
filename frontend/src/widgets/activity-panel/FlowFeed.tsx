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
    <ul className="space-y-2">
      {cards.map((card, index) => {
        const content = (
          <>
            <span className="mr-2 inline-flex items-center rounded-full bg-surface-subtle px-2 py-0.5 text-xs font-medium text-text-secondary">
              {typeLabels[card.type]}
            </span>
            {card.headline}
          </>
        );
        return (
          <li key={index} className="rounded-md border border-border px-3 py-2 text-sm">
            {card.questionId ? (
              <Link href={`/questions/${card.questionId}`} className="hover:underline">
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
