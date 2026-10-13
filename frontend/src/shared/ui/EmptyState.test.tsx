import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { EmptyState } from "./EmptyState";

describe("EmptyState", () => {
  it("renders the message as a dashed box by default", () => {
    render(<EmptyState message="아직 질문이 없습니다." />);
    expect(screen.getByText("아직 질문이 없습니다.")).toHaveClass("border-dashed");
  });

  it("renders a plain line when inline", () => {
    render(<EmptyState message="관련 질문이 없습니다." inline />);
    expect(screen.getByText("관련 질문이 없습니다.")).not.toHaveClass("border-dashed");
  });

  it("renders nothing for an empty message", () => {
    const { container } = render(<EmptyState message="" />);
    expect(container).toBeEmptyDOMElement();
  });
});
