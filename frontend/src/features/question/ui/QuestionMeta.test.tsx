import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { QuestionMeta } from "./QuestionMeta";

const NOW = new Date("2026-01-15T12:00:00.000Z");

describe("QuestionMeta", () => {
  beforeEach(() => {
    vi.useFakeTimers();
    vi.setSystemTime(NOW);
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it("shows only the asked time for an unedited question (version 1)", () => {
    render(
      <QuestionMeta
        questionId={1}
        createdAt={new Date(NOW.getTime() - 60 * 60 * 1000).toISOString()}
        updatedAt={new Date(NOW.getTime() - 60 * 60 * 1000).toISOString()}
        versionNumber={1}
      />,
    );

    expect(screen.getByText(/asked 1시간 전/)).toBeInTheDocument();
    expect(screen.queryByRole("link")).not.toBeInTheDocument();
  });

  it("shows the edited revision link once the question has been revised", () => {
    render(
      <QuestionMeta
        questionId={1}
        createdAt={new Date(NOW.getTime() - 2 * 24 * 60 * 60 * 1000).toISOString()}
        updatedAt={new Date(NOW.getTime() - 60 * 60 * 1000).toISOString()}
        versionNumber={3}
      />,
    );

    const link = screen.getByRole("link", { name: /edited 1시간 전 · revision 3/ });
    expect(link).toHaveAttribute("href", "/questions/1/versions");
  });
});
