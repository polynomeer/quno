import { describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { SearchFilters } from "./SearchFilters";

describe("SearchFilters", () => {
  it("renders a toggle for every question status", () => {
    render(
      <SearchFilters
        availableTags={[]}
        selectedTags={[]}
        onToggleTag={vi.fn()}
        selectedStatuses={[]}
        onToggleStatus={vi.fn()}
      />,
    );

    expect(screen.getByText("Open")).toBeInTheDocument();
    expect(screen.getByText("Needs info")).toBeInTheDocument();
    expect(screen.getByText("Updated")).toBeInTheDocument();
    expect(screen.getByText("Solved")).toBeInTheDocument();
    expect(screen.getByText("Outdated")).toBeInTheDocument();
  });

  it("calls onToggleStatus with the clicked status", async () => {
    const onToggleStatus = vi.fn();
    render(
      <SearchFilters
        availableTags={[]}
        selectedTags={[]}
        onToggleTag={vi.fn()}
        selectedStatuses={[]}
        onToggleStatus={onToggleStatus}
      />,
    );

    await userEvent.click(screen.getByText("Open"));

    expect(onToggleStatus).toHaveBeenCalledWith("OPEN");
  });

  it("dims a status toggle that isn't selected", () => {
    render(
      <SearchFilters
        availableTags={[]}
        selectedTags={[]}
        onToggleTag={vi.fn()}
        selectedStatuses={["OPEN"]}
        onToggleStatus={vi.fn()}
      />,
    );

    expect(screen.getByText("Open").closest("button")).not.toHaveClass("opacity-40");
    expect(screen.getByText("Solved").closest("button")).toHaveClass("opacity-40");
  });

  it("does not render the Tags row when there are no available tags", () => {
    render(
      <SearchFilters
        availableTags={[]}
        selectedTags={[]}
        onToggleTag={vi.fn()}
        selectedStatuses={[]}
        onToggleStatus={vi.fn()}
      />,
    );

    expect(screen.queryByText("Tags")).not.toBeInTheDocument();
  });

  it("renders a toggle for each available tag and calls onToggleTag when clicked", async () => {
    const onToggleTag = vi.fn();
    render(
      <SearchFilters
        availableTags={["kotlin", "spring-boot"]}
        selectedTags={["kotlin"]}
        onToggleTag={onToggleTag}
        selectedStatuses={[]}
        onToggleStatus={vi.fn()}
      />,
    );

    expect(screen.getByText("kotlin")).toHaveClass("bg-brand", "text-brand-foreground");
    expect(screen.getByText("spring-boot")).toHaveClass("bg-surface-subtle");

    await userEvent.click(screen.getByText("spring-boot"));

    expect(onToggleTag).toHaveBeenCalledWith("spring-boot");
  });
});
