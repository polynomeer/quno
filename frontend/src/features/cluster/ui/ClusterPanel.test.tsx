import type { ReactElement } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ClusterPanel } from "./ClusterPanel";
import { clusterApi } from "../api/cluster.api";
import { ApiError } from "@/shared/api/api-error";
import type { ClusterDetail } from "../api/cluster.types";
import type { QuestionSummary } from "@/features/question/api/question.types";

vi.mock("../api/cluster.api", () => ({
  clusterApi: { getForQuestion: vi.fn(), markAsSameProblem: vi.fn(), designateSuperAnswer: vi.fn() },
}));

function member(id: number, title: string): QuestionSummary {
  return { id, title, status: "OPEN", tags: [], score: 0 };
}

function cluster(overrides: Partial<ClusterDetail> = {}): ClusterDetail {
  return { clusterId: 1, members: [], representativeAnswerId: null, ...overrides };
}

function renderWithClient(ui: ReactElement) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(<QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>);
}

describe("ClusterPanel", () => {
  beforeEach(() => {
    vi.mocked(clusterApi.getForQuestion).mockReset();
    vi.mocked(clusterApi.markAsSameProblem).mockReset();
    vi.mocked(clusterApi.designateSuperAnswer).mockReset();
  });

  it("shows the join form even when the question isn't in any cluster yet (404)", async () => {
    vi.mocked(clusterApi.getForQuestion).mockRejectedValue(new ApiError(404, "NOT_FOUND", "not in a cluster"));
    renderWithClient(<ClusterPanel questionId={1} acceptedAnswerId={null} />);

    expect(await screen.findByPlaceholderText("같은 문제인 질문 ID")).toBeInTheDocument();
    expect(screen.queryByText(/Super Answer 지정됨/)).not.toBeInTheDocument();
  });

  it("lists the other members of the cluster, excluding the current question", async () => {
    vi.mocked(clusterApi.getForQuestion).mockResolvedValue(
      cluster({ members: [member(1, "이 질문"), member(2, "같은 클러스터 질문")] }),
    );
    renderWithClient(<ClusterPanel questionId={1} acceptedAnswerId={null} />);

    expect(await screen.findByRole("link", { name: "같은 클러스터 질문" })).toBeInTheDocument();
    expect(screen.queryByRole("link", { name: "이 질문" })).not.toBeInTheDocument();
  });

  it("shows which answer is the designated Super Answer", async () => {
    vi.mocked(clusterApi.getForQuestion).mockResolvedValue(cluster({ representativeAnswerId: 42 }));
    renderWithClient(<ClusterPanel questionId={1} acceptedAnswerId={null} />);

    expect(await screen.findByText("✓ Super Answer 지정됨 (답변 #42)")).toBeInTheDocument();
  });

  it("offers to designate the accepted answer as Super Answer when it isn't already one", async () => {
    vi.mocked(clusterApi.getForQuestion).mockResolvedValue(cluster({ representativeAnswerId: null }));
    renderWithClient(<ClusterPanel questionId={1} acceptedAnswerId={99} />);

    expect(await screen.findByRole("button", { name: "채택된 답변을 Super Answer로 지정" })).toBeInTheDocument();
  });

  it("hides the designate button once the accepted answer is already the Super Answer", async () => {
    vi.mocked(clusterApi.getForQuestion).mockResolvedValue(cluster({ representativeAnswerId: 99 }));
    renderWithClient(<ClusterPanel questionId={1} acceptedAnswerId={99} />);

    await screen.findByText(/Super Answer 지정됨/);
    expect(screen.queryByRole("button", { name: "채택된 답변을 Super Answer로 지정" })).not.toBeInTheDocument();
  });

  it("keeps 같은 문제로 표시 disabled until a question id is entered", async () => {
    vi.mocked(clusterApi.getForQuestion).mockRejectedValue(new ApiError(404, "NOT_FOUND", "x"));
    renderWithClient(<ClusterPanel questionId={1} acceptedAnswerId={null} />);
    await screen.findByPlaceholderText("같은 문제인 질문 ID");

    expect(screen.getByRole("button", { name: "같은 문제로 표시" })).toBeDisabled();

    await userEvent.type(screen.getByPlaceholderText("같은 문제인 질문 ID"), "5");

    expect(screen.getByRole("button", { name: "같은 문제로 표시" })).toBeEnabled();
  });

  it("marks the entered question id as the same problem and clears the input on success", async () => {
    vi.mocked(clusterApi.getForQuestion).mockRejectedValue(new ApiError(404, "NOT_FOUND", "x"));
    vi.mocked(clusterApi.markAsSameProblem).mockResolvedValue({ clusterId: 1, memberQuestionIds: [1, 5], representativeAnswerId: null });
    renderWithClient(<ClusterPanel questionId={1} acceptedAnswerId={null} />);
    const input = await screen.findByPlaceholderText("같은 문제인 질문 ID");
    await userEvent.type(input, "5");

    await userEvent.click(screen.getByRole("button", { name: "같은 문제로 표시" }));

    await waitFor(() => expect(clusterApi.markAsSameProblem).toHaveBeenCalledWith(1, 5));
    await waitFor(() => expect(input).toHaveValue(""));
  });

  it("shows the backend's error message when designating a Super Answer fails", async () => {
    vi.mocked(clusterApi.getForQuestion).mockResolvedValue(cluster({ representativeAnswerId: null }));
    vi.mocked(clusterApi.designateSuperAnswer).mockRejectedValue(new ApiError(403, "FORBIDDEN", "권한이 없습니다."));
    renderWithClient(<ClusterPanel questionId={1} acceptedAnswerId={99} />);
    await userEvent.click(await screen.findByRole("button", { name: "채택된 답변을 Super Answer로 지정" }));

    expect(await screen.findByText("권한이 없습니다.")).toBeInTheDocument();
  });
});
