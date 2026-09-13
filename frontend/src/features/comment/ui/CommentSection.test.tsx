import type { ReactElement } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { CommentSection } from "./CommentSection";
import { useSession } from "@/features/auth/hooks/useSession";
import { commentApi } from "../api/comment.api";
import { ApiError } from "@/shared/api/api-error";
import type { Comment } from "../api/comment.types";
import type { MyProfile } from "@/features/auth/api/auth.types";

vi.mock("@/features/auth/hooks/useSession", () => ({ useSession: vi.fn() }));

vi.mock("../api/comment.api", () => ({
  commentApi: { list: vi.fn(), create: vi.fn(), edit: vi.fn(), remove: vi.fn(), getVersions: vi.fn() },
}));

const ME: MyProfile = { id: 1, email: "me@example.com", nickname: "me", acceptsDirectAsk: false, createdAt: "" };

function comment(overrides: Partial<Comment> = {}): Comment {
  return {
    id: 1,
    targetType: "QUESTION",
    targetId: 1,
    authorId: ME.id,
    parentCommentId: null,
    body: "첫 댓글입니다",
    versionNumber: 1,
    isDeleted: false,
    createdAt: "2026-01-01T00:00:00.000Z",
    updatedAt: "2026-01-01T00:00:00.000Z",
    ...overrides,
  };
}

function renderWithClient(ui: ReactElement) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(<QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>);
}

describe("CommentSection", () => {
  beforeEach(() => {
    vi.mocked(useSession).mockReturnValue({ data: ME } as ReturnType<typeof useSession>);
    vi.mocked(commentApi.list).mockReset().mockResolvedValue([]);
    vi.mocked(commentApi.create).mockReset();
    vi.mocked(commentApi.edit).mockReset();
    vi.mocked(commentApi.remove).mockReset();
  });

  it("shows every top-level comment's body", async () => {
    vi.mocked(commentApi.list).mockResolvedValue([comment({ id: 1, body: "첫 댓글" }), comment({ id: 2, body: "둘째 댓글" })]);
    renderWithClient(<CommentSection targetType="QUESTION" targetId={1} />);

    expect(await screen.findByText("첫 댓글")).toBeInTheDocument();
    expect(screen.getByText("둘째 댓글")).toBeInTheDocument();
  });

  it("nests a reply under its parent comment", async () => {
    vi.mocked(commentApi.list).mockResolvedValue([
      comment({ id: 1, body: "부모 댓글" }),
      comment({ id: 2, body: "답글 내용입니다", parentCommentId: 1 }),
    ]);
    renderWithClient(<CommentSection targetType="QUESTION" targetId={1} />);

    expect(await screen.findByText("답글 내용입니다")).toBeInTheDocument();
  });

  it("shows a placeholder for a deleted comment instead of its body", async () => {
    vi.mocked(commentApi.list).mockResolvedValue([comment({ body: null, isDeleted: true })]);
    renderWithClient(<CommentSection targetType="QUESTION" targetId={1} />);

    expect(await screen.findByText("삭제된 댓글입니다")).toBeInTheDocument();
  });

  it("hides the 'Add a comment' toggle for an anonymous viewer", async () => {
    vi.mocked(useSession).mockReturnValue({ data: undefined } as ReturnType<typeof useSession>);
    renderWithClient(<CommentSection targetType="QUESTION" targetId={1} />);

    await waitFor(() => expect(commentApi.list).toHaveBeenCalled());
    expect(screen.queryByRole("button", { name: "Add a comment" })).not.toBeInTheDocument();
  });

  it("reveals the composer and posts a new top-level comment", async () => {
    vi.mocked(commentApi.create).mockResolvedValue(comment());
    renderWithClient(<CommentSection targetType="QUESTION" targetId={1} />);
    await userEvent.click(await screen.findByRole("button", { name: "Add a comment" }));

    await userEvent.type(screen.getByPlaceholderText("댓글을 입력하세요"), "새 댓글");
    await userEvent.click(screen.getByRole("button", { name: "Comment" }));

    await waitFor(() => expect(commentApi.create).toHaveBeenCalledWith("QUESTION", 1, "새 댓글", undefined));
  });

  it("closes the composer on 취소 without posting", async () => {
    renderWithClient(<CommentSection targetType="QUESTION" targetId={1} />);
    await userEvent.click(await screen.findByRole("button", { name: "Add a comment" }));
    await userEvent.type(screen.getByPlaceholderText("댓글을 입력하세요"), "안 보낼 댓글");

    await userEvent.click(screen.getByRole("button", { name: "취소" }));

    expect(screen.queryByPlaceholderText("댓글을 입력하세요")).not.toBeInTheDocument();
    expect(commentApi.create).not.toHaveBeenCalled();
  });

  it("shows the backend's error message when posting a comment fails", async () => {
    vi.mocked(commentApi.create).mockRejectedValue(new ApiError(400, "TOO_LONG", "댓글이 너무 깁니다."));
    renderWithClient(<CommentSection targetType="QUESTION" targetId={1} />);
    await userEvent.click(await screen.findByRole("button", { name: "Add a comment" }));
    await userEvent.type(screen.getByPlaceholderText("댓글을 입력하세요"), "새 댓글");

    await userEvent.click(screen.getByRole("button", { name: "Comment" }));

    expect(await screen.findByText("댓글이 너무 깁니다.")).toBeInTheDocument();
  });

  it("lets the comment's own author edit it", async () => {
    vi.mocked(commentApi.list).mockResolvedValue([comment({ body: "원본" })]);
    vi.mocked(commentApi.edit).mockResolvedValue(comment({ body: "수정됨" }));
    renderWithClient(<CommentSection targetType="QUESTION" targetId={1} />);
    await userEvent.click(await screen.findByText("수정"));

    const textarea = screen.getByDisplayValue("원본");
    await userEvent.clear(textarea);
    await userEvent.type(textarea, "수정됨");
    await userEvent.click(screen.getByRole("button", { name: "저장" }));

    await waitFor(() => expect(commentApi.edit).toHaveBeenCalledWith(1, "수정됨"));
  });

  it("lets the comment's own author delete it", async () => {
    vi.mocked(commentApi.list).mockResolvedValue([comment()]);
    vi.mocked(commentApi.remove).mockResolvedValue(undefined);
    renderWithClient(<CommentSection targetType="QUESTION" targetId={1} />);

    await userEvent.click(await screen.findByText("삭제"));

    await waitFor(() => expect(commentApi.remove).toHaveBeenCalledWith(1));
  });

  it("does not show edit/delete controls for someone else's comment", async () => {
    vi.mocked(commentApi.list).mockResolvedValue([comment({ authorId: 999 })]);
    renderWithClient(<CommentSection targetType="QUESTION" targetId={1} />);

    await screen.findByText("첫 댓글입니다");
    expect(screen.queryByText("수정")).not.toBeInTheDocument();
    expect(screen.queryByText("삭제")).not.toBeInTheDocument();
  });
});
