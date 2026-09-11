import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { TagChip } from "./TagChip";

describe("TagChip", () => {
  it("renders the tag name as a link to its tag detail page", () => {
    render(<TagChip name="kotlin" />);

    const link = screen.getByRole("link", { name: "kotlin" });
    expect(link).toHaveAttribute("href", "/tags/kotlin");
  });

  it("merges an extra className", () => {
    render(<TagChip name="spring-boot" className="mr-1" />);
    expect(screen.getByRole("link", { name: "spring-boot" })).toHaveClass("mr-1");
  });
});
