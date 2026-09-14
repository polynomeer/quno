import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { TagContributorList } from "./TagContributorList";
import type { TagContributor } from "@/entities/tag/model/tag.types";

function contributor(userId: number, nickname: string, answerCount: number): TagContributor {
  return { userId, nickname, answerCount };
}

describe("TagContributorList", () => {
  it("shows the empty message when there are no contributors", () => {
    render(<TagContributorList contributors={[]} />);
    expect(screen.getByText("아직 답변자가 없습니다.")).toBeInTheDocument();
  });

  it("ranks contributors in the given order, starting at 1", () => {
    render(<TagContributorList contributors={[contributor(1, "alice", 10), contributor(2, "bob", 5)]} />);

    const items = screen.getAllByRole("listitem");
    expect(items[0]).toHaveTextContent("1");
    expect(items[0]).toHaveTextContent("alice");
    expect(items[0]).toHaveTextContent("답변 10개");
    expect(items[1]).toHaveTextContent("2");
    expect(items[1]).toHaveTextContent("bob");
  });

  it("links each contributor's nickname to their profile", () => {
    render(<TagContributorList contributors={[contributor(1, "alice", 10)]} />);
    expect(screen.getByRole("link", { name: "alice" })).toHaveAttribute("href", "/users/1");
  });
});
