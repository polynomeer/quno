import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { QuestionCard } from "./QuestionCard";
import type { QuestionSummary } from "@/features/question/api/question.types";

const question: QuestionSummary = {
  id: 42,
  title: "Spring Boot 4에서 빈 등록이 안 되는 이유",
  status: "OPEN",
  tags: ["spring-boot", "kotlin"],
  score: 7,
  answerCount: 3,
  hasAcceptedAnswer: false,
  versionNumber: 4,
  viewCount: 1834,
  createdAt: "2026-01-01T00:00:00Z",
  updatedAt: "2026-01-02T00:00:00Z",
};

describe("QuestionCard", () => {
  it("links the title to the question detail page", () => {
    render(
      <ul>
        <QuestionCard question={question} />
      </ul>,
    );

    expect(screen.getByRole("link", { name: question.title })).toHaveAttribute("href", "/questions/42");
  });

  it("shows the status badge, score, answer count and revision", () => {
    render(
      <ul>
        <QuestionCard question={question} />
      </ul>,
    );

    expect(screen.getByText("Open")).toBeInTheDocument();
    expect(screen.getByText("7")).toBeInTheDocument();
    expect(screen.getByText("3")).toBeInTheDocument();
    expect(screen.getByText("rev 4")).toBeInTheDocument();
    expect(screen.getByText("1.8k")).toBeInTheDocument();
  });

  it("hides the revision chip for an unrevised question", () => {
    render(
      <ul>
        <QuestionCard question={{ ...question, versionNumber: 1 }} />
      </ul>,
    );

    expect(screen.queryByText(/^rev /)).not.toBeInTheDocument();
  });

  it("renders a tag chip for every tag", () => {
    render(
      <ul>
        <QuestionCard question={question} />
      </ul>,
    );

    expect(screen.getByRole("link", { name: "spring-boot" })).toHaveAttribute("href", "/tags/spring-boot");
    expect(screen.getByRole("link", { name: "kotlin" })).toHaveAttribute("href", "/tags/kotlin");
  });

  it("renders no tag chips when the question has none", () => {
    render(
      <ul>
        <QuestionCard question={{ ...question, tags: [] }} />
      </ul>,
    );

    expect(screen.queryByRole("link", { name: "spring-boot" })).not.toBeInTheDocument();
  });
});
