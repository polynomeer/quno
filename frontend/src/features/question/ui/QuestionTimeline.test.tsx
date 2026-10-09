import type { ReactElement } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { QuestionTimeline } from "./QuestionTimeline";
import { questionApi } from "../api/question.api";
import type { QuestionTimelineEvent } from "../api/question.types";

vi.mock("../api/question.api", () => ({
  questionApi: { timeline: vi.fn() },
}));

function event(overrides: Partial<QuestionTimelineEvent>): QuestionTimelineEvent {
  return {
    type: "QUESTION_CREATED",
    occurredAt: "2026-01-01T00:00:00Z",
    actorId: 1,
    versionNumber: 1,
    answerId: null,
    accepted: false,
    ...overrides,
  };
}

function renderWithClient(ui: ReactElement) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(<QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>);
}

describe("QuestionTimeline", () => {
  beforeEach(() => {
    vi.mocked(questionApi.timeline).mockReset();
  });

  it("renders nothing when there are no events", async () => {
    vi.mocked(questionApi.timeline).mockResolvedValue([]);
    const { container } = renderWithClient(<QuestionTimeline questionId={1} />);

    await waitFor(() => expect(questionApi.timeline).toHaveBeenCalledWith(1));
    expect(container).toBeEmptyDOMElement();
  });

  it("labels each event and links answers to their card", async () => {
    vi.mocked(questionApi.timeline).mockResolvedValue([
      event({ type: "QUESTION_REVISED", versionNumber: 2, occurredAt: "2026-01-03T00:00:00Z" }),
      event({ type: "ANSWER_POSTED", answerId: 7, accepted: true, actorId: 2, occurredAt: "2026-01-02T00:00:00Z" }),
      event({ type: "REVIEW_ADDRESSED", actorId: null, versionNumber: null }),
      event({ type: "QUESTION_CREATED" }),
    ]);
    renderWithClient(<QuestionTimeline questionId={1} />);

    expect(await screen.findByText("rev 2 리비전")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "답변 · 수락됨" })).toHaveAttribute("href", "#answer-7");
    expect(screen.getByText("QPR 요청 반영됨")).toBeInTheDocument();
    expect(screen.getByText("질문 등록 · rev 1")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "리비전 비교 (diff) →" })).toHaveAttribute("href", "/questions/1/versions");
  });

  it("omits the diff link when the question was never revised", async () => {
    vi.mocked(questionApi.timeline).mockResolvedValue([event({ type: "QUESTION_CREATED" })]);
    renderWithClient(<QuestionTimeline questionId={1} />);

    await screen.findByText("질문 등록 · rev 1");
    expect(screen.queryByRole("link", { name: /리비전 비교/ })).not.toBeInTheDocument();
  });
});
