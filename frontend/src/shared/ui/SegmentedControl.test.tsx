import { describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { SegmentedControl } from "./SegmentedControl";

const options = [
  { value: "popular", label: "인기", count: 5 },
  { value: "resolved", label: "오늘 해결" },
] as const;

describe("SegmentedControl", () => {
  it("groups the options under the given label and marks the selected one as pressed", () => {
    render(<SegmentedControl options={[...options]} value="popular" onChange={() => {}} label="피드 필터" />);

    expect(screen.getByRole("group", { name: "피드 필터" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "인기 5" })).toHaveAttribute("aria-pressed", "true");
    expect(screen.getByRole("button", { name: "오늘 해결" })).toHaveAttribute("aria-pressed", "false");
  });

  it("reports the clicked option's value", async () => {
    const onChange = vi.fn();
    render(<SegmentedControl options={[...options]} value="popular" onChange={onChange} label="피드 필터" />);

    await userEvent.click(screen.getByRole("button", { name: "오늘 해결" }));

    expect(onChange).toHaveBeenCalledWith("resolved");
  });
});
