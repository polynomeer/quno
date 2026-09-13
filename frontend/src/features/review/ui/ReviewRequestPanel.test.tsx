import type { ReactElement } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ReviewRequestPanel } from "./ReviewRequestPanel";
import { reviewApi } from "../api/review.api";
import { ApiError } from "@/shared/api/api-error";
import type { ReviewRequest } from "../api/review.types";

const AUTHOR_ID = 1;
const REVIEWER_ID = 2;

vi.mock("../api/review.api", () => ({
  reviewApi: { list: vi.fn(), create: vi.fn(), reRequest: vi.fn() },
}));

function request(overrides: Partial<ReviewRequest> = {}): ReviewRequest {
  return {
    id: 1,
    questionId: 10,
    requestedBy: REVIEWER_ID,
    message: "환경 정보를 추가해주세요",
    status: "OPEN",
    questionVersionNumberAtRequest: 1,
    createdAt: "2026-01-01T00:00:00.000Z",
    addressedAt: null,
    ...overrides,
  };
}

function renderWithClient(ui: ReactElement) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(<QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>);
}

describe("ReviewRequestPanel", () => {
  beforeEach(() => {
    vi.mocked(reviewApi.list).mockReset().mockResolvedValue([]);
    vi.mocked(reviewApi.create).mockReset();
    vi.mocked(reviewApi.reRequest).mockReset();
  });

  it("renders nothing for the author when there are no requests yet", async () => {
    const { container } = renderWithClient(
      <ReviewRequestPanel questionId={10} questionAuthorId={AUTHOR_ID} questionVersionNumber={1} currentUserId={AUTHOR_ID} />,
    );

    await waitFor(() => expect(reviewApi.list).toHaveBeenCalledWith(10));
    expect(container).toBeEmptyDOMElement();
  });

  it("shows the request form for a non-author even with no requests yet", async () => {
    renderWithClient(
      <ReviewRequestPanel questionId={10} questionAuthorId={AUTHOR_ID} questionVersionNumber={1} currentUserId={REVIEWER_ID} />,
    );

    expect(await screen.findByPlaceholderText("이 질문에 추가로 필요한 정보를 요청하세요")).toBeInTheDocument();
  });

  it("does not show the request form for the question's own author", async () => {
    vi.mocked(reviewApi.list).mockResolvedValue([request()]);
    renderWithClient(
      <ReviewRequestPanel questionId={10} questionAuthorId={AUTHOR_ID} questionVersionNumber={1} currentUserId={AUTHOR_ID} />,
    );

    await screen.findByText("환경 정보를 추가해주세요");
    expect(screen.queryByPlaceholderText("이 질문에 추가로 필요한 정보를 요청하세요")).not.toBeInTheDocument();
  });

  it("lists existing requests with their status", async () => {
    vi.mocked(reviewApi.list).mockResolvedValue([request({ status: "OPEN" }), request({ id: 2, status: "ADDRESSED" })]);
    renderWithClient(
      <ReviewRequestPanel questionId={10} questionAuthorId={AUTHOR_ID} questionVersionNumber={1} currentUserId={AUTHOR_ID} />,
    );

    expect(await screen.findByText("OPEN")).toBeInTheDocument();
    expect(screen.getByText("ADDRESSED")).toBeInTheDocument();
  });

  it("keeps 정보 요청 disabled until a message is entered", async () => {
    renderWithClient(
      <ReviewRequestPanel questionId={10} questionAuthorId={AUTHOR_ID} questionVersionNumber={1} currentUserId={REVIEWER_ID} />,
    );

    expect(screen.getByRole("button", { name: "정보 요청" })).toBeDisabled();

    await userEvent.type(screen.getByPlaceholderText("이 질문에 추가로 필요한 정보를 요청하세요"), "메시지");

    expect(screen.getByRole("button", { name: "정보 요청" })).toBeEnabled();
  });

  it("submits the request and clears the textarea on success", async () => {
    vi.mocked(reviewApi.create).mockResolvedValue(request());
    renderWithClient(
      <ReviewRequestPanel questionId={10} questionAuthorId={AUTHOR_ID} questionVersionNumber={1} currentUserId={REVIEWER_ID} />,
    );
    const textarea = screen.getByPlaceholderText("이 질문에 추가로 필요한 정보를 요청하세요");
    await userEvent.type(textarea, "환경 정보가 필요합니다");

    await userEvent.click(screen.getByRole("button", { name: "정보 요청" }));

    await waitFor(() => expect(reviewApi.create).toHaveBeenCalledWith(10, "환경 정보가 필요합니다"));
    await waitFor(() => expect(textarea).toHaveValue(""));
  });

  it("shows a 재요청 처리 button only when the author has since updated the question", async () => {
    vi.mocked(reviewApi.list).mockResolvedValue([request({ questionVersionNumberAtRequest: 1 })]);
    const { rerender } = renderWithClient(
      <ReviewRequestPanel questionId={10} questionAuthorId={AUTHOR_ID} questionVersionNumber={1} currentUserId={AUTHOR_ID} />,
    );
    await screen.findByText("OPEN");
    expect(screen.queryByRole("button", { name: "재요청 처리" })).not.toBeInTheDocument();

    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    rerender(
      <QueryClientProvider client={queryClient}>
        <ReviewRequestPanel questionId={10} questionAuthorId={AUTHOR_ID} questionVersionNumber={2} currentUserId={AUTHOR_ID} />
      </QueryClientProvider>,
    );

    expect(await screen.findByRole("button", { name: "재요청 처리" })).toBeInTheDocument();
  });

  it("shows the backend's error message when creating a request fails", async () => {
    vi.mocked(reviewApi.create).mockRejectedValue(new ApiError(429, "TOO_MANY_REQUESTS", "요청이 너무 많습니다."));
    renderWithClient(
      <ReviewRequestPanel questionId={10} questionAuthorId={AUTHOR_ID} questionVersionNumber={1} currentUserId={REVIEWER_ID} />,
    );
    await userEvent.type(screen.getByPlaceholderText("이 질문에 추가로 필요한 정보를 요청하세요"), "메시지");

    await userEvent.click(screen.getByRole("button", { name: "정보 요청" }));

    expect(await screen.findByText("요청이 너무 많습니다.")).toBeInTheDocument();
  });
});
