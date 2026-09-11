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

  it("shows the status badge and score", () => {
    render(
      <ul>
        <QuestionCard question={question} />
      </ul>,
    );

    expect(screen.getByText("Open")).toBeInTheDocument();
    expect(screen.getByText("score 7")).toBeInTheDocument();
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
