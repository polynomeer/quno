import type { ReactElement } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { VoteControl } from "./VoteControl";
import { authApi } from "@/features/auth/api/auth.api";
import { voteApi } from "../api/vote.api";
import { tokenStorage } from "@/shared/lib/token-storage";
import type { MyProfile } from "@/features/auth/api/auth.types";

vi.mock("@/features/auth/api/auth.api", () => ({
  authApi: { me: vi.fn() },
}));

vi.mock("../api/vote.api", () => ({
  voteApi: { myVotes: vi.fn(), cast: vi.fn(), retract: vi.fn() },
}));

const ME: MyProfile = { id: 1, email: "me@example.com", nickname: "me", acceptsDirectAsk: false, createdAt: "" };

function renderWithClient(ui: ReactElement) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(<QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>);
}

function logIn() {
  tokenStorage.setTokens("access-token", "refresh-token");
}

describe("VoteControl", () => {
  beforeEach(() => {
    vi.mocked(authApi.me).mockReset().mockResolvedValue(ME);
    vi.mocked(voteApi.myVotes).mockReset().mockResolvedValue([]);
    vi.mocked(voteApi.cast).mockReset().mockResolvedValue(undefined);
    vi.mocked(voteApi.retract).mockReset().mockResolvedValue(undefined);
    tokenStorage.clear();
  });

  it("shows only the plain score for an anonymous viewer (no vote buttons)", () => {
    renderWithClient(<VoteControl targetType="QUESTION" targetId={10} questionId={10} score={5} authorId={99} />);

    expect(screen.getByText("5")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Upvote" })).not.toBeInTheDocument();
  });

  it("shows only the plain score when the viewer is the author (self-vote is blocked backend-side)", async () => {
    logIn();
    renderWithClient(<VoteControl targetType="QUESTION" targetId={10} questionId={10} score={5} authorId={ME.id} />);

    await waitFor(() => expect(authApi.me).toHaveBeenCalled());
    expect(screen.getByText("5")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Upvote" })).not.toBeInTheDocument();
  });

  it("shows vote buttons for a logged-in non-author viewer", async () => {
    logIn();
    renderWithClient(<VoteControl targetType="QUESTION" targetId={10} questionId={10} score={5} authorId={99} />);

    expect(await screen.findByRole("button", { name: "Upvote" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Downvote" })).toBeInTheDocument();
  });

  it("casts an upvote when the viewer has not voted yet", async () => {
    logIn();
    renderWithClient(<VoteControl targetType="QUESTION" targetId={10} questionId={10} score={5} authorId={99} />);

    await userEvent.click(await screen.findByRole("button", { name: "Upvote" }));

    await waitFor(() => expect(voteApi.cast).toHaveBeenCalledWith("QUESTION", 10, 1));
    expect(voteApi.retract).not.toHaveBeenCalled();
  });

  it("retracts the vote when clicking the already-selected direction again", async () => {
    logIn();
    vi.mocked(voteApi.myVotes).mockResolvedValue([{ targetType: "QUESTION", targetId: 10, value: 1 }]);
    renderWithClient(<VoteControl targetType="QUESTION" targetId={10} questionId={10} score={6} authorId={99} />);

    const upvote = await screen.findByRole("button", { name: "Upvote" });
    await waitFor(() => expect(upvote).toHaveClass("text-brand"));

    await userEvent.click(upvote);

    await waitFor(() => expect(voteApi.retract).toHaveBeenCalled());
    expect(voteApi.cast).not.toHaveBeenCalled();
  });
});
