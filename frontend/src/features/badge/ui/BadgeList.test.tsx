import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { BadgeList } from "./BadgeList";
import type { Badge } from "../api/badge.types";

describe("BadgeList", () => {
  it("shows the empty message when there are no badges", () => {
    render(<BadgeList badges={[]} emptyMessage="아직 배지가 없습니다." />);
    expect(screen.getByText("아직 배지가 없습니다.")).toBeInTheDocument();
  });

  it("renders a chip for every badge", () => {
    const badges: Badge[] = [
      { type: "FIRST_QUESTION", tier: "BRONZE" },
      { type: "PROBLEM_SOLVER", tier: "SILVER" },
    ];
    render(<BadgeList badges={badges} emptyMessage="" />);

    expect(screen.getByText("첫 질문")).toBeInTheDocument();
    expect(screen.getByText("문제 해결사")).toBeInTheDocument();
  });
});
