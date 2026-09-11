import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { BadgeChip } from "./BadgeChip";
import type { Badge } from "../api/badge.types";

describe("BadgeChip", () => {
  it("shows the Korean display name for the badge type", () => {
    const badge: Badge = { type: "FIRST_QUESTION", tier: "BRONZE" };
    render(<BadgeChip badge={badge} />);

    expect(screen.getByText("첫 질문")).toBeInTheDocument();
  });

  it("uses the badge description as its title (tooltip)", () => {
    const badge: Badge = { type: "PROBLEM_SOLVER", tier: "SILVER" };
    render(<BadgeChip badge={badge} />);

    expect(screen.getByText("문제 해결사")).toHaveAttribute("title", "채택된 답변을 5개 이상 보유하고 있습니다");
  });

  it("uses the dedicated brand-subtle tone for the GOLD tier (WCAG AA fix, ADR-0051)", () => {
    const badge: Badge = { type: "SUPER_ANSWER", tier: "GOLD" };
    render(<BadgeChip badge={badge} />);

    expect(screen.getByText("Super Answer")).toHaveClass("bg-brand-subtle", "text-brand");
  });
});
