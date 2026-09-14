import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { TrendingTagsPanel } from "./TrendingTagsPanel";
import type { TagSpike, TagTrend } from "@/features/dashboard/api/dashboard.types";

function trend(overrides: Partial<TagTrend> = {}): TagTrend {
  return { id: 1, name: "kotlin", slug: "kotlin", questionCount: 12, ...overrides };
}

function spike(overrides: Partial<TagSpike> = {}): TagSpike {
  return { id: 2, name: "spring-boot", slug: "spring-boot", recentCount: 20, baselineAveragePerDay: 5, spikeRatio: 4, ...overrides };
}

describe("TrendingTagsPanel", () => {
  it("renders nothing when there are no trends or spikes", () => {
    const { container } = render(<TrendingTagsPanel tags={[]} spikes={[]} />);
    expect(container).toBeEmptyDOMElement();
  });

  it("shows Trending Tags with each tag's question count", () => {
    render(<TrendingTagsPanel tags={[trend()]} spikes={[]} />);

    expect(screen.getByText("Trending Tags")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "kotlin" })).toBeInTheDocument();
    expect(screen.getByText("12개 질문")).toBeInTheDocument();
    expect(screen.queryByText("Trending Errors")).not.toBeInTheDocument();
  });

  it("shows Trending Errors with the spike ratio formatted to one decimal", () => {
    render(<TrendingTagsPanel tags={[]} spikes={[spike({ spikeRatio: 3.456 })]} />);

    expect(screen.getByText("Trending Errors")).toBeInTheDocument();
    expect(screen.getByText("3.5x 급증")).toBeInTheDocument();
    expect(screen.queryByText("Trending Tags")).not.toBeInTheDocument();
  });

  it("shows both sections when both trends and spikes are present", () => {
    render(<TrendingTagsPanel tags={[trend()]} spikes={[spike()]} />);

    expect(screen.getByText("Trending Tags")).toBeInTheDocument();
    expect(screen.getByText("Trending Errors")).toBeInTheDocument();
  });
});
