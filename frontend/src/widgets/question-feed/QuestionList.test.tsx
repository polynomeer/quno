import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { QuestionList } from "./QuestionList";
import type { QuestionSummary } from "@/features/question/api/question.types";

const questions: QuestionSummary[] = [
  { id: 1, title: "첫 번째 질문", status: "OPEN", tags: [], score: 1 },
  { id: 2, title: "두 번째 질문", status: "RESOLVED", tags: [], score: 5 },
];

describe("QuestionList", () => {
  it("shows the empty message when there are no questions", () => {
    render(<QuestionList questions={[]} emptyMessage="아직 질문이 없습니다." />);

    expect(screen.getByText("아직 질문이 없습니다.")).toBeInTheDocument();
    expect(screen.queryByRole("list")).not.toBeInTheDocument();
  });

  it("renders one card per question, in order", () => {
    render(<QuestionList questions={questions} emptyMessage="아직 질문이 없습니다." />);

    const links = screen.getAllByRole("link", { name: /번째 질문/ });
    expect(links).toHaveLength(2);
    expect(links[0]).toHaveTextContent("첫 번째 질문");
    expect(links[1]).toHaveTextContent("두 번째 질문");
  });
});
