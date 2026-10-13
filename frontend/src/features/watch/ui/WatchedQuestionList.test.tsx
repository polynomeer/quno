import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { WatchedQuestionList } from "./WatchedQuestionList";

describe("WatchedQuestionList", () => {
  it("links each watched question and shows its status", () => {
    render(
      <WatchedQuestionList
        questions={[{ questionId: 3, title: "R2DBC 커넥션 풀 진단", status: "UPDATED" }]}
        emptyMessage="없음"
      />,
    );

    expect(screen.getByRole("link", { name: "R2DBC 커넥션 풀 진단" })).toHaveAttribute("href", "/questions/3");
    expect(screen.getByText("Updated")).toBeInTheDocument();
  });

  it("shows the empty message instead of an empty list", () => {
    render(<WatchedQuestionList questions={[]} emptyMessage="아직 Watch 중인 질문이 없습니다." />);

    expect(screen.getByText("아직 Watch 중인 질문이 없습니다.")).toBeInTheDocument();
    expect(screen.queryByRole("list")).not.toBeInTheDocument();
  });
});
