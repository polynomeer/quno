import type { ReactElement } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { TagDetailsEditor } from "./TagDetailsEditor";
import { useSession } from "@/features/auth/hooks/useSession";
import { tagApi } from "@/entities/tag/api/tag.api";
import { ApiError } from "@/shared/api/api-error";
import type { MyProfile } from "@/features/auth/api/auth.types";
import type { Tag } from "@/entities/tag/model/tag.types";

vi.mock("@/features/auth/hooks/useSession", () => ({ useSession: vi.fn() }));

vi.mock("@/entities/tag/api/tag.api", () => ({
  tagApi: { updateDetails: vi.fn() },
}));

const ME: MyProfile = { id: 1, email: "me@example.com", nickname: "me", acceptsDirectAsk: false, createdAt: "" };

const TAG: Tag = { id: 5, name: "kotlin", slug: "kotlin", description: null, docsUrl: null };

function renderWithClient(ui: ReactElement) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(<QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>);
}

describe("TagDetailsEditor", () => {
  beforeEach(() => {
    vi.mocked(tagApi.updateDetails).mockReset();
  });

  it("shows the placeholder text when the tag has no description or docs link", () => {
    vi.mocked(useSession).mockReturnValue({ data: undefined } as ReturnType<typeof useSession>);
    renderWithClient(<TagDetailsEditor tag={TAG} />);

    expect(screen.getByText("아직 설명이 없습니다.")).toBeInTheDocument();
  });

  it("shows the existing description and docs link", () => {
    vi.mocked(useSession).mockReturnValue({ data: undefined } as ReturnType<typeof useSession>);
    renderWithClient(<TagDetailsEditor tag={{ ...TAG, description: "코틀린 언어 태그", docsUrl: "https://kotlinlang.org" }} />);

    expect(screen.getByText("코틀린 언어 태그")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "공식 문서 →" })).toHaveAttribute("href", "https://kotlinlang.org");
  });

  it("hides the 편집 trigger for an anonymous viewer", () => {
    vi.mocked(useSession).mockReturnValue({ data: undefined } as ReturnType<typeof useSession>);
    renderWithClient(<TagDetailsEditor tag={TAG} />);

    expect(screen.queryByRole("button", { name: "편집" })).not.toBeInTheDocument();
  });

  it("switches to edit mode with the current values pre-filled", async () => {
    vi.mocked(useSession).mockReturnValue({ data: ME } as ReturnType<typeof useSession>);
    renderWithClient(<TagDetailsEditor tag={{ ...TAG, description: "코틀린 언어 태그", docsUrl: "https://kotlinlang.org" }} />);

    await userEvent.click(screen.getByRole("button", { name: "편집" }));

    expect(screen.getByPlaceholderText("이 태그에 대한 설명")).toHaveValue("코틀린 언어 태그");
    expect(screen.getByPlaceholderText("공식 문서 URL (선택)")).toHaveValue("https://kotlinlang.org");
  });

  it("cancels back to the original values without saving", async () => {
    vi.mocked(useSession).mockReturnValue({ data: ME } as ReturnType<typeof useSession>);
    renderWithClient(<TagDetailsEditor tag={{ ...TAG, description: "원래 설명" }} />);
    await userEvent.click(screen.getByRole("button", { name: "편집" }));

    await userEvent.clear(screen.getByPlaceholderText("이 태그에 대한 설명"));
    await userEvent.type(screen.getByPlaceholderText("이 태그에 대한 설명"), "임시로 바꿔본 설명");
    await userEvent.click(screen.getByRole("button", { name: "취소" }));

    expect(screen.getByText("원래 설명")).toBeInTheDocument();
    expect(tagApi.updateDetails).not.toHaveBeenCalled();
  });

  it("saves the trimmed description and docs URL", async () => {
    vi.mocked(useSession).mockReturnValue({ data: ME } as ReturnType<typeof useSession>);
    vi.mocked(tagApi.updateDetails).mockResolvedValue({ ...TAG, description: "새 설명", docsUrl: "https://example.com" });
    renderWithClient(<TagDetailsEditor tag={TAG} />);
    await userEvent.click(screen.getByRole("button", { name: "편집" }));

    await userEvent.type(screen.getByPlaceholderText("이 태그에 대한 설명"), "  새 설명  ");
    await userEvent.type(screen.getByPlaceholderText("공식 문서 URL (선택)"), "  https://example.com  ");
    await userEvent.click(screen.getByRole("button", { name: "저장" }));

    await waitFor(() =>
      expect(tagApi.updateDetails).toHaveBeenCalledWith(TAG.id, { description: "새 설명", docsUrl: "https://example.com" }),
    );
  });

  it("stays in edit mode and shows the backend's error message when saving fails", async () => {
    vi.mocked(useSession).mockReturnValue({ data: ME } as ReturnType<typeof useSession>);
    vi.mocked(tagApi.updateDetails).mockRejectedValue(new ApiError(409, "CONFLICT", "동시 수정이 감지되었습니다."));
    renderWithClient(<TagDetailsEditor tag={TAG} />);
    await userEvent.click(screen.getByRole("button", { name: "편집" }));

    await userEvent.click(screen.getByRole("button", { name: "저장" }));

    expect(await screen.findByText("동시 수정이 감지되었습니다.")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "저장" })).toBeInTheDocument();
  });
});
