import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { FollowingList } from "./FollowingList";
import type { Followee } from "../api/follow.types";

describe("FollowingList", () => {
  it("shows the empty message when there are no users", () => {
    render(<FollowingList users={[]} emptyMessage="팔로우하는 사용자가 없습니다." />);
    expect(screen.getByText("팔로우하는 사용자가 없습니다.")).toBeInTheDocument();
  });

  it("links each user's nickname to their profile", () => {
    const users: Followee[] = [
      { userId: 1, nickname: "alice" },
      { userId: 2, nickname: "bob" },
    ];
    render(<FollowingList users={users} emptyMessage="" />);

    expect(screen.getByRole("link", { name: "alice" })).toHaveAttribute("href", "/users/1");
    expect(screen.getByRole("link", { name: "bob" })).toHaveAttribute("href", "/users/2");
  });
});
