import { beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QuestionDetailContent } from "./QuestionDetailContent";
import { ApiError } from "@/shared/api/api-error";
import { useSession } from "@/features/auth/hooks/useSession";
import { useQuestion } from "@/features/question/hooks/useQuestion";
import { useRelatedQuestions } from "@/features/question/hooks/useRelatedQuestions";
import { useAnswers } from "@/features/answer/hooks/useAnswers";
import { useAcceptAnswer } from "@/features/answer/hooks/useAcceptAnswer";
import type { MyProfile } from "@/features/auth/api/auth.types";
import type { QuestionDetail } from "@/features/question/api/question.types";
import type { Answer } from "@/features/answer/api/answer.types";

vi.mock("@/features/auth/hooks/useSession", () => ({ useSession: vi.fn() }));
vi.mock("@/features/question/hooks/useQuestion", () => ({ useQuestion: vi.fn() }));
vi.mock("@/features/question/hooks/useRelatedQuestions", () => ({ useRelatedQuestions: vi.fn() }));
vi.mock("@/features/answer/hooks/useAnswers", () => ({ useAnswers: vi.fn() }));
vi.mock("@/features/answer/hooks/useAcceptAnswer", () => ({ useAcceptAnswer: vi.fn() }));

// Every one of these has its own dedicated test file — QuestionDetailContent's own job is
// wiring (loading/error states, answer sorting, login-gated sections), not their internals.
vi.mock("@/features/question/ui/OutdatedAction", () => ({ OutdatedAction: () => <div data-testid="outdated-action" /> }));
vi.mock("@/features/answer/ui/AnswerComposer", () => ({ AnswerComposer: () => <div data-testid="answer-composer" /> }));
vi.mock("@/features/watch/ui/WatchButton", () => ({ WatchButton: () => <div data-testid="watch-button" /> }));
vi.mock("@/features/save/ui/SaveButton", () => ({ SaveButton: () => <div data-testid="save-button" /> }));
vi.mock("@/features/vote/ui/VoteControl", () => ({ VoteControl: () => <div data-testid="vote-control" /> }));
vi.mock("@/features/comment/ui/CommentSection", () => ({ CommentSection: () => <div data-testid="comment-section" /> }));
vi.mock("@/features/report/ui/ReportButton", () => ({ ReportButton: () => <div data-testid="report-button" /> }));
vi.mock("@/features/review/ui/ReviewRequestPanel", () => ({ ReviewRequestPanel: () => <div data-testid="review-panel" /> }));
vi.mock("@/features/cluster/ui/ClusterPanel", () => ({ ClusterPanel: () => <div data-testid="cluster-panel" /> }));
vi.mock("@/features/question/ui/ForkPanel", () => ({ ForkPanel: () => <div data-testid="fork-panel" /> }));
vi.mock("@/features/live-chat/ui/LiveChatPanel", () => ({ LiveChatPanel: () => <div data-testid="live-chat-panel" /> }));
vi.mock("@/features/answer/ui/AnswerCard", () => ({
  AnswerCard: ({ answer, canAccept, onAccept, isAccepting }: { answer: Answer; canAccept?: boolean; onAccept?: () => void; isAccepting?: boolean }) => (
    <li data-testid={`answer-${answer.id}`}>
      {answer.body} {answer.isAccepted && "(accepted)"}
      {canAccept && !answer.isAccepted && (
        <button onClick={onAccept} disabled={isAccepting}>
          Accept {answer.id}
        </button>
      )}
    </li>
  ),
}));

const AUTHOR: MyProfile = { id: 1, email: "author@example.com", nickname: "author", acceptsDirectAsk: false, createdAt: "" };
const VIEWER: MyProfile = { id: 2, email: "viewer@example.com", nickname: "viewer", acceptsDirectAsk: false, createdAt: "" };

const QUESTION: QuestionDetail = {
  id: 10,
  authorId: AUTHOR.id,
  title: "Spring Boot 4에서 빈 등록이 안 되는 이유",
  status: "OPEN",
  versionNumber: 1,
  body: "질문 본문",
  environment: null,
  logs: null,
  tags: ["spring-boot"],
  createdAt: "2026-01-01T00:00:00.000Z",
  updatedAt: "2026-01-01T00:00:00.000Z",
  score: 3,
};

function answer(overrides: Partial<Answer> = {}): Answer {
  return {
    id: 1,
    questionId: QUESTION.id,
    authorId: VIEWER.id,
    body: "답변",
    isAccepted: false,
    targetVersionNumber: 1,
    isStale: false,
    createdAt: "2026-01-01T00:00:00.000Z",
    updatedAt: "2026-01-01T00:00:00.000Z",
    score: 0,
    ...overrides,
  };
}

function mockAcceptAnswer(overrides: Partial<ReturnType<typeof useAcceptAnswer>> = {}) {
  vi.mocked(useAcceptAnswer).mockReturnValue({
    mutate: vi.fn(),
    isPending: false,
    variables: undefined,
    error: null,
    ...overrides,
  } as unknown as ReturnType<typeof useAcceptAnswer>);
}

describe("QuestionDetailContent", () => {
  beforeEach(() => {
    vi.mocked(useRelatedQuestions).mockReturnValue({ data: [] } as unknown as ReturnType<typeof useRelatedQuestions>);
    vi.mocked(useAnswers).mockReturnValue({ data: [] } as unknown as ReturnType<typeof useAnswers>);
    mockAcceptAnswer();
  });

  it("shows a loading skeleton while the question is loading", () => {
    vi.mocked(useSession).mockReturnValue({ data: undefined, isLoading: false } as ReturnType<typeof useSession>);
    vi.mocked(useQuestion).mockReturnValue({ data: undefined, isLoading: true, isError: false } as unknown as ReturnType<typeof useQuestion>);

    render(<QuestionDetailContent questionId={10} />);

    expect(screen.queryByRole("heading")).not.toBeInTheDocument();
  });

  it("shows a not-found message when the question genuinely doesn't exist (404)", () => {
    vi.mocked(useSession).mockReturnValue({ data: undefined, isLoading: false } as ReturnType<typeof useSession>);
    vi.mocked(useQuestion).mockReturnValue({
      data: undefined,
      isLoading: false,
      isError: true,
      error: new ApiError(404, "QUESTION_NOT_FOUND", "not found"),
    } as unknown as ReturnType<typeof useQuestion>);

    render(<QuestionDetailContent questionId={10} />);

    expect(screen.getByText("질문을 찾을 수 없습니다.")).toBeInTheDocument();
  });

  // 장애 시나리오 F1(백엔드 완전 다운) — 연결 자체가 안 되면 fetch가 순수 TypeError를 던지고
  // ApiError가 아니므로, "삭제된 질문"과는 다른 메시지를 보여줘야 한다.
  it("shows a generic retry message when the question fails to load for a non-404 reason", () => {
    vi.mocked(useSession).mockReturnValue({ data: undefined, isLoading: false } as ReturnType<typeof useSession>);
    vi.mocked(useQuestion).mockReturnValue({
      data: undefined,
      isLoading: false,
      isError: true,
      error: new TypeError("Failed to fetch"),
    } as unknown as ReturnType<typeof useQuestion>);

    render(<QuestionDetailContent questionId={10} />);

    expect(screen.getByText("질문을 불러오지 못했습니다. 잠시 후 다시 시도해주세요.")).toBeInTheDocument();
    expect(screen.queryByText("질문을 찾을 수 없습니다.")).not.toBeInTheDocument();
  });

  it("renders the question title and body once loaded", () => {
    vi.mocked(useSession).mockReturnValue({ data: undefined, isLoading: false } as ReturnType<typeof useSession>);
    vi.mocked(useQuestion).mockReturnValue({ data: QUESTION, isLoading: false, isError: false } as unknown as ReturnType<typeof useQuestion>);

    render(<QuestionDetailContent questionId={10} />);

    expect(screen.getByRole("heading", { name: QUESTION.title })).toBeInTheDocument();
    expect(screen.getByText("질문 본문")).toBeInTheDocument();
  });

  it("prompts an anonymous viewer to log in instead of showing the answer composer", () => {
    vi.mocked(useSession).mockReturnValue({ data: undefined, isLoading: false } as ReturnType<typeof useSession>);
    vi.mocked(useQuestion).mockReturnValue({ data: QUESTION, isLoading: false, isError: false } as unknown as ReturnType<typeof useQuestion>);

    render(<QuestionDetailContent questionId={10} />);

    expect(screen.queryByTestId("answer-composer")).not.toBeInTheDocument();
    expect(screen.getByRole("link", { name: "로그인" })).toHaveAttribute("href", "/login?redirectTo=/questions/10");
    expect(screen.queryByTestId("watch-button")).not.toBeInTheDocument();
    expect(screen.queryByTestId("cluster-panel")).not.toBeInTheDocument();
  });

  it("shows the answer composer and member-only panels for a logged-in viewer", () => {
    vi.mocked(useSession).mockReturnValue({ data: VIEWER, isLoading: false } as ReturnType<typeof useSession>);
    vi.mocked(useQuestion).mockReturnValue({ data: QUESTION, isLoading: false, isError: false } as unknown as ReturnType<typeof useQuestion>);

    render(<QuestionDetailContent questionId={10} />);

    expect(screen.getByTestId("answer-composer")).toBeInTheDocument();
    expect(screen.getByTestId("watch-button")).toBeInTheDocument();
    expect(screen.getByTestId("cluster-panel")).toBeInTheDocument();
    expect(screen.getByTestId("fork-panel")).toBeInTheDocument();
    expect(screen.getByTestId("live-chat-panel")).toBeInTheDocument();
  });

  it("shows Report/Outdated actions for a viewer who isn't the question's author", () => {
    vi.mocked(useSession).mockReturnValue({ data: VIEWER, isLoading: false } as ReturnType<typeof useSession>);
    vi.mocked(useQuestion).mockReturnValue({ data: QUESTION, isLoading: false, isError: false } as unknown as ReturnType<typeof useQuestion>);

    render(<QuestionDetailContent questionId={10} />);

    expect(screen.getByTestId("report-button")).toBeInTheDocument();
    expect(screen.getByTestId("outdated-action")).toBeInTheDocument();
  });

  it("hides the Report button for the question's own author", () => {
    vi.mocked(useSession).mockReturnValue({ data: AUTHOR, isLoading: false } as ReturnType<typeof useSession>);
    vi.mocked(useQuestion).mockReturnValue({ data: QUESTION, isLoading: false, isError: false } as unknown as ReturnType<typeof useQuestion>);

    render(<QuestionDetailContent questionId={10} />);

    expect(screen.queryByTestId("report-button")).not.toBeInTheDocument();
  });

  it("sorts accepted answers first under the default 'best' sort", () => {
    vi.mocked(useSession).mockReturnValue({ data: undefined, isLoading: false } as ReturnType<typeof useSession>);
    vi.mocked(useQuestion).mockReturnValue({ data: QUESTION, isLoading: false, isError: false } as unknown as ReturnType<typeof useQuestion>);
    vi.mocked(useAnswers).mockReturnValue({
      data: [answer({ id: 1, body: "먼저 쓴 답변", isAccepted: false }), answer({ id: 2, body: "채택된 답변", isAccepted: true })],
    } as unknown as ReturnType<typeof useAnswers>);

    render(<QuestionDetailContent questionId={10} />);

    const items = screen.getAllByTestId(/^answer-/);
    expect(items[0]).toHaveTextContent("채택된 답변");
    expect(items[1]).toHaveTextContent("먼저 쓴 답변");
  });

  it("sorts by score when 'Score' is selected", async () => {
    vi.mocked(useSession).mockReturnValue({ data: undefined, isLoading: false } as ReturnType<typeof useSession>);
    vi.mocked(useQuestion).mockReturnValue({ data: QUESTION, isLoading: false, isError: false } as unknown as ReturnType<typeof useQuestion>);
    vi.mocked(useAnswers).mockReturnValue({
      data: [answer({ id: 1, body: "낮은 점수", score: 1 }), answer({ id: 2, body: "높은 점수", score: 10 })],
    } as unknown as ReturnType<typeof useAnswers>);

    render(<QuestionDetailContent questionId={10} />);
    await userEvent.selectOptions(screen.getByRole("combobox"), "score");

    const items = screen.getAllByTestId(/^answer-/);
    expect(items[0]).toHaveTextContent("높은 점수");
    expect(items[1]).toHaveTextContent("낮은 점수");
  });

  it("hides the sort dropdown when there's 0 or 1 answer", () => {
    vi.mocked(useSession).mockReturnValue({ data: undefined, isLoading: false } as ReturnType<typeof useSession>);
    vi.mocked(useQuestion).mockReturnValue({ data: QUESTION, isLoading: false, isError: false } as unknown as ReturnType<typeof useQuestion>);
    vi.mocked(useAnswers).mockReturnValue({ data: [answer()] } as unknown as ReturnType<typeof useAnswers>);

    render(<QuestionDetailContent questionId={10} />);

    expect(screen.queryByRole("combobox")).not.toBeInTheDocument();
    expect(screen.getByText("1 Answers")).toBeInTheDocument();
  });

  it("shows '아직 답변이 없습니다' when there are no answers", () => {
    vi.mocked(useSession).mockReturnValue({ data: undefined, isLoading: false } as ReturnType<typeof useSession>);
    vi.mocked(useQuestion).mockReturnValue({ data: QUESTION, isLoading: false, isError: false } as unknown as ReturnType<typeof useQuestion>);

    render(<QuestionDetailContent questionId={10} />);

    expect(screen.getByText("아직 답변이 없습니다.")).toBeInTheDocument();
  });

  it("lets only the question's author accept an answer", async () => {
    const mutate = vi.fn();
    mockAcceptAnswer({ mutate });
    vi.mocked(useSession).mockReturnValue({ data: AUTHOR, isLoading: false } as ReturnType<typeof useSession>);
    vi.mocked(useQuestion).mockReturnValue({ data: QUESTION, isLoading: false, isError: false } as unknown as ReturnType<typeof useQuestion>);
    vi.mocked(useAnswers).mockReturnValue({ data: [answer({ id: 5 })] } as unknown as ReturnType<typeof useAnswers>);

    render(<QuestionDetailContent questionId={10} />);
    await userEvent.click(screen.getByRole("button", { name: "Accept 5" }));

    expect(mutate).toHaveBeenCalledWith(5);
  });

  it("does not offer an accept button to a non-author viewer", () => {
    vi.mocked(useSession).mockReturnValue({ data: VIEWER, isLoading: false } as ReturnType<typeof useSession>);
    vi.mocked(useQuestion).mockReturnValue({ data: QUESTION, isLoading: false, isError: false } as unknown as ReturnType<typeof useQuestion>);
    vi.mocked(useAnswers).mockReturnValue({ data: [answer({ id: 5 })] } as unknown as ReturnType<typeof useAnswers>);

    render(<QuestionDetailContent questionId={10} />);

    expect(screen.queryByRole("button", { name: "Accept 5" })).not.toBeInTheDocument();
  });

  it("shows the environment/logs disclosure only when either is present", () => {
    vi.mocked(useSession).mockReturnValue({ data: undefined, isLoading: false } as ReturnType<typeof useSession>);
    vi.mocked(useQuestion).mockReturnValue({
      data: { ...QUESTION, environment: "Spring Boot 4.0.8" },
      isLoading: false,
      isError: false,
    } as unknown as ReturnType<typeof useQuestion>);

    render(<QuestionDetailContent questionId={10} />);

    expect(screen.getByText("Spring Boot 4.0.8")).toBeInTheDocument();
  });
});
