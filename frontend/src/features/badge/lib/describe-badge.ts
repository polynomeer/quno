import type { BadgeType } from "../api/badge.types";

interface DescribedBadge {
  name: string;
  description: string;
}

/** Backend only sends the identifier (domain/badge/BadgeType) — thresholds mirror ADR-0027. */
const badges: Record<BadgeType, DescribedBadge> = {
  FIRST_QUESTION: { name: "첫 질문", description: "질문을 1개 이상 작성했습니다" },
  FIRST_ANSWER: { name: "첫 답변", description: "답변을 1개 이상 작성했습니다" },
  PROBLEM_SOLVER: { name: "문제 해결사", description: "채택된 답변을 5개 이상 보유하고 있습니다" },
  WELL_RECEIVED: { name: "호평받는 기여자", description: "작성한 질문·답변이 받은 투표 점수 합이 50점 이상입니다" },
  TRUSTED_ANSWERER: { name: "신뢰받는 답변자", description: "채택된 답변을 20개 이상 보유하고 있습니다" },
  SUPER_ANSWER: { name: "Super Answer", description: "Super Answer로 지정된 답변이 있습니다" },
};

export function describeBadge(type: BadgeType): DescribedBadge {
  return badges[type];
}
