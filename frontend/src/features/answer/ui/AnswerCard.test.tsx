import type { ReactElement } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { AnswerCard } from "./AnswerCard";
import { useSession } from "@/features/auth/hooks/useSession";
import { answerApi } from "../api/answer.api";
import { ApiError } from "@/shared/api/api-error";
import type { Answer, AnswerMutationResult } from "../api/answer.types";
import type { MyProfile } from "@/features/auth/api/auth.types";

vi.mock("@/features/auth/hooks/useSession", () => ({ useSession: vi.fn() }));

vi.mock("../api/answer.api", () => ({
  answerApi: { listVersions: vi.fn(), revise: vi.fn() },
}));

// AnswerCard's own logic (badges, edit toggle, save/cancel) is what these tests target —
// VoteControl/CommentSection/ReportButton have their own dedicated tests, so they're stubbed here.
vi.mock("@/features/vote/ui/VoteControl", () => ({ VoteControl: () => <div data-testid="vote-control" /> }));
vi.mock("@/features/comment/ui/CommentSection", () => ({ CommentSection: () => <div data-testid="comment-section" /> }));
vi.mock("@/features/report/ui/ReportButton", () => ({ ReportButton: () => <div data-testid="report-button" /> }));

const ME: MyProfile = { id: 1, email: "me@example.com", nickname: "me", acceptsDirectAsk: false, createdAt: "" };

const ANSWER: Answer = {
  id: 10,
  questionId: 1,
  authorId: ME.id,
  body: "original body",
  isAccepted: false,
  targetVersionNumber: 1,
  isStale: false,
  createdAt: "2026-01-01T00:00:00.000Z",
  updatedAt: "2026-01-01T00:00:00.000Z",
  score: 3,
};

const REVISE_RESULT: AnswerMutationResult = { id: ANSWER.id, questionId: ANSWER.questionId, versionNumber: 2 };

function renderWithClient(ui: ReactElement) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  const result = render(<QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>);
  return {
    ...result,
    rerender: (next: ReactElement) => result.rerender(<QueryClientProvider client={queryClient}>{next}</QueryClientProvider>),
  };
}

describe("AnswerCard", () => {
  beforeEach(() => {
    vi.mocked(useSession).mockReturnValue({ data: ME } as ReturnType<typeof useSession>);
    vi.mocked(answerApi.listVersions).mockReset().mockResolvedValue([]);
    vi.mocked(answerApi.revise).mockReset().mockResolvedValue(REVISE_RESULT);
  });

  it("renders the answer body as markdown when not editing", () => {
    renderWithClient(<AnswerCard answer={ANSWER} showEngagement />);
    expect(screen.getByText("original body")).toBeInTheDocument();
  });

  it("shows the Accepted badge only when the answer is accepted", () => {
    const { rerender } = renderWithClient(<AnswerCard answer={ANSWER} showEngagement />);
    expect(screen.queryByText("✓ Accepted")).not.toBeInTheDocument();

    rerender(<AnswerCard answer={{ ...ANSWER, isAccepted: true }} showEngagement />);
    expect(screen.getByText("✓ Accepted")).toBeInTheDocument();
  });

  it("shows the stale badge when the answer targets an outdated question version", () => {
    renderWithClient(<AnswerCard answer={{ ...ANSWER, isStale: true, targetVersionNumber: 2 }} showEngagement />);
    expect(screen.getByText(/질문이 이후 수정됨/)).toBeInTheDocument();
  });

  it("does not show engagement controls when showEngagement is false", () => {
    renderWithClient(<AnswerCard answer={ANSWER} />);
    expect(screen.queryByTestId("vote-control")).not.toBeInTheDocument();
    expect(screen.queryByTestId("comment-section")).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Edit" })).not.toBeInTheDocument();
  });

  it("shows the Edit trigger for the answer's own author", () => {
    renderWithClient(<AnswerCard answer={ANSWER} showEngagement />);
    expect(screen.getByRole("button", { name: "Edit" })).toBeInTheDocument();
  });

  it("hides the Edit trigger for a viewer who isn't the answer's author", () => {
    vi.mocked(useSession).mockReturnValue({ data: { ...ME, id: 999 } } as ReturnType<typeof useSession>);
    renderWithClient(<AnswerCard answer={ANSWER} showEngagement />);
    expect(screen.queryByRole("button", { name: "Edit" })).not.toBeInTheDocument();
  });

  it("switches to edit mode with the current body pre-filled", async () => {
    renderWithClient(<AnswerCard answer={ANSWER} showEngagement />);

    await userEvent.click(screen.getByRole("button", { name: "Edit" }));

    expect(screen.getByRole("textbox")).toHaveValue("original body");
  });

  it("cancels back to read-only view without saving", async () => {
    renderWithClient(<AnswerCard answer={ANSWER} showEngagement />);
    await userEvent.click(screen.getByRole("button", { name: "Edit" }));

    await userEvent.click(screen.getByRole("button", { name: "Cancel" }));

    expect(screen.queryByRole("textbox")).not.toBeInTheDocument();
    expect(screen.getByText("original body")).toBeInTheDocument();
  });

  it("saves the revised body and returns to read-only view on success", async () => {
    renderWithClient(<AnswerCard answer={ANSWER} showEngagement />);
    await userEvent.click(screen.getByRole("button", { name: "Edit" }));

    await userEvent.clear(screen.getByRole("textbox"));
    await userEvent.type(screen.getByRole("textbox"), "revised body");
    await userEvent.click(screen.getByRole("button", { name: "Save" }));

    expect(answerApi.revise).toHaveBeenCalledWith(ANSWER.id, "revised body");
    // The parent list (useAnswers), not this prop, is what actually reflects the new body after
    // the mutation invalidates it — here we only verify this card itself leaves edit mode.
    await waitFor(() => expect(screen.queryByRole("textbox")).not.toBeInTheDocument());
  });

  it("stays in edit mode and shows the error when saving fails (regression: missing try/catch)", async () => {
    vi.mocked(answerApi.revise).mockRejectedValue(new ApiError(403, "FORBIDDEN", "수정 권한이 없습니다."));
    renderWithClient(<AnswerCard answer={ANSWER} showEngagement />);
    await userEvent.click(screen.getByRole("button", { name: "Edit" }));

    await userEvent.click(screen.getByRole("button", { name: "Save" }));

    expect(await screen.findByText("수정 권한이 없습니다.")).toBeInTheDocument();
    expect(screen.getByRole("textbox")).toBeInTheDocument();
  });
});
