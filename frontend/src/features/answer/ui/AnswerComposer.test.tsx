import type { ReactElement } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { AnswerComposer } from "./AnswerComposer";
import { answerApi } from "../api/answer.api";
import { ApiError } from "@/shared/api/api-error";
import type { Answer } from "../api/answer.types";

vi.mock("../api/answer.api", () => ({
  answerApi: { create: vi.fn() },
}));

const NEW_ANSWER: Answer = {
  id: 1,
  questionId: 10,
  authorId: 1,
  body: "답변 내용",
  isAccepted: false,
  targetVersionNumber: 1,
  isStale: false,
  createdAt: "",
  updatedAt: "",
  score: 0,
};

function renderWithClient(ui: ReactElement) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(<QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>);
}

describe("AnswerComposer", () => {
  beforeEach(() => {
    vi.mocked(answerApi.create).mockReset();
  });

  it("keeps Post Answer disabled until the body has content", async () => {
    renderWithClient(<AnswerComposer questionId={10} />);

    expect(screen.getByRole("button", { name: "Post Answer" })).toBeDisabled();

    await userEvent.type(screen.getByRole("textbox"), "제 생각에는...");

    expect(screen.getByRole("button", { name: "Post Answer" })).toBeEnabled();
  });

  it("submits the trimmed body and clears the editor on success", async () => {
    vi.mocked(answerApi.create).mockResolvedValue(NEW_ANSWER);
    renderWithClient(<AnswerComposer questionId={10} />);
    const textarea = screen.getByRole("textbox");
    await userEvent.type(textarea, "  답변 내용  ");

    await userEvent.click(screen.getByRole("button", { name: "Post Answer" }));

    await waitFor(() => expect(answerApi.create).toHaveBeenCalledWith(10, "답변 내용"));
    await waitFor(() => expect(textarea).toHaveValue(""));
  });

  it("keeps the draft and shows the backend's error message when submitting fails", async () => {
    vi.mocked(answerApi.create).mockRejectedValue(new ApiError(403, "FORBIDDEN", "답변을 작성할 수 없습니다."));
    renderWithClient(<AnswerComposer questionId={10} />);
    const textarea = screen.getByRole("textbox");
    await userEvent.type(textarea, "답변 내용");

    await userEvent.click(screen.getByRole("button", { name: "Post Answer" }));

    expect(await screen.findByText("답변을 작성할 수 없습니다.")).toBeInTheDocument();
    expect(textarea).toHaveValue("답변 내용");
  });
});
