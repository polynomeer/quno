import { describe, expect, it } from "vitest";
import { render } from "@testing-library/react";
import { Skeleton } from "./Skeleton";

describe("Skeleton", () => {
  it("renders a pulsing placeholder block", () => {
    const { container } = render(<Skeleton />);
    expect(container.firstChild).toHaveClass("animate-pulse", "rounded-md", "bg-surface-subtle");
  });

  it("merges an extra className for sizing", () => {
    const { container } = render(<Skeleton className="h-40 w-full" />);
    expect(container.firstChild).toHaveClass("animate-pulse", "h-40", "w-full");
  });
});
