import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { SavedQuestionList } from "./SavedQuestionList";

describe("SavedQuestionList", () => {
  it("links each saved question and shows its status", () => {
    render(
      <SavedQuestionList
        questions={[{ questionId: 4, title: "Testcontainers 재사용 모드", status: "RESOLVED" }]}
        emptyMessage="없음"
      />,
    );

    expect(screen.getByRole("link", { name: "Testcontainers 재사용 모드" })).toHaveAttribute("href", "/questions/4");
    expect(screen.getByText("Solved")).toBeInTheDocument();
  });

  it("shows the empty message instead of an empty list", () => {
    render(<SavedQuestionList questions={[]} emptyMessage="아직 저장한 질문이 없습니다." />);

    expect(screen.getByText("아직 저장한 질문이 없습니다.")).toBeInTheDocument();
    expect(screen.queryByRole("list")).not.toBeInTheDocument();
  });
});
