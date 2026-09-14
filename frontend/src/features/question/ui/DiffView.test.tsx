import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { DiffView } from "./DiffView";
import type { DiffLine } from "../api/question.types";

describe("DiffView", () => {
  it("renders each line with its diff prefix", () => {
    const lines: DiffLine[] = [
      { type: "EQUAL", text: "변경 없는 줄" },
      { type: "ADDED", text: "추가된 줄" },
      { type: "REMOVED", text: "삭제된 줄" },
    ];
    const { container } = render(<DiffView lines={lines} />);

    const rendered = Array.from(container.querySelectorAll("pre > div")).map((el) =>
      (el.textContent ?? "").replace(/\s+/g, " ").trim(),
    );
    expect(rendered).toEqual(["변경 없는 줄", "+ 추가된 줄", "- 삭제된 줄"]);
  });

  it("gives added/removed lines their distinct tone classes", () => {
    const lines: DiffLine[] = [
      { type: "ADDED", text: "추가" },
      { type: "REMOVED", text: "삭제" },
    ];
    render(<DiffView lines={lines} />);

    expect(screen.getByText("+ 추가")).toHaveClass("bg-success-subtle", "text-success");
    expect(screen.getByText("- 삭제")).toHaveClass("bg-danger-subtle", "text-danger");
  });

  it("renders no lines when given an empty diff", () => {
    const { container } = render(<DiffView lines={[]} />);
    expect(container.querySelector("pre")).toBeEmptyDOMElement();
  });
});
