import type { ReactElement } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ForkPanel } from "./ForkPanel";
import { questionApi } from "../api/question.api";
import { ApiError } from "@/shared/api/api-error";
import type { QuestionGraph, QuestionSummary } from "../api/question.types";

const push = vi.fn();

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push }),
}));

vi.mock("../api/question.api", () => ({
  questionApi: { getGraph: vi.fn(), fork: vi.fn() },
}));

function summary(id: number, title: string): QuestionSummary {
  return { id, title, status: "OPEN", tags: [], score: 0 };
}

function graphOf(overrides: Partial<QuestionGraph>): QuestionGraph {
  return { questionId: 1, clusterMembers: [], forkedFrom: null, forks: [], relatedQuestions: [], ...overrides };
}

function renderWithClient(ui: ReactElement) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(<QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>);
}

describe("ForkPanel", () => {
  beforeEach(() => {
    push.mockReset();
    vi.mocked(questionApi.getGraph).mockReset().mockResolvedValue(graphOf({}));
    vi.mocked(questionApi.fork).mockReset();
  });

  it("shows nothing extra when there is no fork lineage", async () => {
    renderWithClient(<ForkPanel questionId={1} />);

    await waitFor(() => expect(questionApi.getGraph).toHaveBeenCalledWith(1));
    expect(screen.queryByText(/Forked from/)).not.toBeInTheDocument();
    expect(screen.queryByText(/Forks \(/)).not.toBeInTheDocument();
  });

  it("shows the parent link when the question was forked from another one", async () => {
    vi.mocked(questionApi.getGraph).mockResolvedValue(graphOf({ forkedFrom: summary(5, "Original question") }));
    renderWithClient(<ForkPanel questionId={1} />);

    expect(await screen.findByRole("link", { name: "Original question" })).toHaveAttribute("href", "/questions/5");
  });

  it("lists every fork of this question", async () => {
    vi.mocked(questionApi.getGraph).mockResolvedValue(
      graphOf({ forks: [summary(2, "Fork A"), summary(3, "Fork B")] }),
    );
    renderWithClient(<ForkPanel questionId={1} />);

    expect(await screen.findByText("Forks (2)")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Fork A" })).toHaveAttribute("href", "/questions/2");
    expect(screen.getByRole("link", { name: "Fork B" })).toHaveAttribute("href", "/questions/3");
  });

  it("forks the question and navigates to the new question on success", async () => {
    vi.mocked(questionApi.fork).mockResolvedValue({ id: 99, title: "t", status: "OPEN", versionNumber: 1 });
    renderWithClient(<ForkPanel questionId={1} />);

    await userEvent.click(await screen.findByRole("button", { name: "Fork this question" }));

    await waitFor(() => expect(push).toHaveBeenCalledWith("/questions/99"));
  });

  it("shows the backend's error message when forking fails, without navigating", async () => {
    vi.mocked(questionApi.fork).mockRejectedValue(new ApiError(409, "CONFLICT", "이미 포크된 질문입니다."));
    renderWithClient(<ForkPanel questionId={1} />);

    await userEvent.click(await screen.findByRole("button", { name: "Fork this question" }));

    expect(await screen.findByText("이미 포크된 질문입니다.")).toBeInTheDocument();
    expect(push).not.toHaveBeenCalled();
  });
});
