import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { FlowFeed } from "./FlowFeed";
import type { FlowCard } from "@/features/dashboard/api/dashboard.types";

function card(overrides: Partial<FlowCard> = {}): FlowCard {
  return { type: "POPULAR_QUESTION", headline: "인기 질문 헤드라인", questionId: 10, clusterId: null, ...overrides };
}

describe("FlowFeed", () => {
  it("shows the empty message when there are no cards", () => {
    render(<FlowFeed cards={[]} />);
    expect(screen.getByText("아직 활동이 없습니다.")).toBeInTheDocument();
  });

  it("shows the type label and headline for each card", () => {
    render(<FlowFeed cards={[card({ type: "TAG_SPIKE", headline: "kotlin 태그 급증" })]} />);

    expect(screen.getByText("태그 급증")).toBeInTheDocument();
    expect(screen.getByText("kotlin 태그 급증")).toBeInTheDocument();
  });

  it("links to the question when the card has a questionId", () => {
    render(<FlowFeed cards={[card({ questionId: 10 })]} />);
    expect(screen.getByRole("link")).toHaveAttribute("href", "/questions/10");
  });

  it("renders without a link when the card has no questionId", () => {
    render(<FlowFeed cards={[card({ questionId: null, type: "CLUSTER_SUPER_ANSWER" })]} />);
    expect(screen.queryByRole("link")).not.toBeInTheDocument();
    expect(screen.getByText("Super Answer")).toBeInTheDocument();
  });
});
