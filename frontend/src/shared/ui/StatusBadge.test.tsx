import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { StatusBadge } from "./StatusBadge";

describe("StatusBadge", () => {
  it.each([
    ["OPEN", "Open"],
    ["NEEDS_INFO", "Needs info"],
    ["UPDATED", "Updated"],
    ["RESOLVED", "Solved"],
    ["OUTDATED", "Outdated"],
  ] as const)("renders the %s status as '%s'", (status, label) => {
    render(<StatusBadge status={status} />);
    expect(screen.getByText(label)).toBeInTheDocument();
  });

  it("uses the dedicated brand-subtle tone for UPDATED (WCAG AA fix, ADR-0051)", () => {
    render(<StatusBadge status="UPDATED" />);
    expect(screen.getByText("Updated")).toHaveClass("bg-brand-subtle", "text-brand");
  });

  it("merges an extra className", () => {
    render(<StatusBadge status="OPEN" className="ml-2" />);
    expect(screen.getByText("Open")).toHaveClass("ml-2");
  });
});
