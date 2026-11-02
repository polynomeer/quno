import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import type { PromotionFinding } from "../api/question.types";
import { PromotionFindings } from "./PromotionFindings";

const finding = (overrides: Partial<PromotionFinding>): PromotionFinding => ({
  field: "body",
  label: "조직 식별자",
  start: 0,
  end: 4,
  excerpt: "Acme",
  severity: "BLOCK",
  ...overrides,
});

describe("PromotionFindings", () => {
  it("separates what must go from what the author should check", () => {
    render(
      <PromotionFindings
        checking={false}
        findings={[
          finding({}),
          finding({ field: "logs", label: "사설 IP 주소", start: 5, excerpt: "10.0.0.1", severity: "WARN" }),
        ]}
      />,
    );
    expect(screen.getByRole("heading", { name: "지워야 해요 1" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "확인해 주세요 1" })).toBeInTheDocument();
    expect(screen.getByText("본문 · 조직 식별자")).toBeInTheDocument();
    expect(screen.getByText("10.0.0.1")).toBeInTheDocument();
  });

  it("says when nothing was found", () => {
    render(<PromotionFindings checking={false} findings={[]} />);
    expect(screen.getByText(/찾은 항목이 없어요/)).toBeInTheDocument();
  });
});
