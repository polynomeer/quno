import { Suspense } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { act, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import LiveChatToAnswerPage from "./page";
import { useRequireAuth } from "@/features/auth/hooks/useRequireAuth";
import { liveChatApi } from "@/features/live-chat/api/live-chat.api";
import { answerApi } from "@/features/answer/api/answer.api";
import { ApiError } from "@/shared/api/api-error";

const push = vi.fn();
vi.mock("next/navigation", () => ({ useRouter: () => ({ push }) }));
vi.mock("@/features/auth/hooks/useRequireAuth", () => ({ useRequireAuth: vi.fn() }));
vi.mock("@/features/live-chat/api/live-chat.api", () => ({
  liveChatApi: { getRoom: vi.fn(), messages: vi.fn(), answerDraft: vi.fn() },
}));
vi.mock("@/features/answer/api/answer.api", () => ({ answerApi: { create: vi.fn() } }));

const MESSAGES = [
  { id: "m1", roomId: 5, senderId: 2, senderNickname: "devpark", body: "블로킹 호출이 섞였나요?", createdAt: "2026-10-22T01:00:00Z" },
  { id: "m2", roomId: 5, senderId: 1, senderNickname: "minji", body: "감사 로그만 JDBC예요.", createdAt: "2026-10-22T01:01:00Z" },
];

async function renderPage() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  await act(async () => {
    render(
      <QueryClientProvider client={client}>
        <Suspense fallback={null}>
          <LiveChatToAnswerPage params={Promise.resolve({ id: "10" })} searchParams={Promise.resolve({})} />
        </Suspense>
      </QueryClientProvider>,
    );
  });
}

describe("LiveChatToAnswerPage", () => {
  beforeEach(() => {
    push.mockReset();
    vi.mocked(useRequireAuth).mockReturnValue({ me: { id: 1 }, isLoading: false } as ReturnType<typeof useRequireAuth>);
    vi.mocked(liveChatApi.getRoom).mockReset().mockResolvedValue({ id: 5, questionId: 10, createdBy: 1, createdAt: "" });
    vi.mocked(liveChatApi.messages).mockReset().mockResolvedValue(MESSAGES);
    vi.mocked(liveChatApi.answerDraft).mockReset();
    vi.mocked(answerApi.create).mockReset();
  });

  it("drafts from the picked messages, then publishes citing the room", async () => {
    vi.mocked(liveChatApi.answerDraft).mockResolvedValue({ roomId: 5, questionId: 10, body: "초안 본문", messageCount: 1 });
    vi.mocked(answerApi.create).mockResolvedValue({ id: 77 } as Awaited<ReturnType<typeof answerApi.create>>);
    await renderPage();

    await userEvent.click(await screen.findByRole("checkbox", { name: /블로킹 호출이 섞였나요/ }));
    await userEvent.click(screen.getByRole("button", { name: "선택한 메시지로 초안 만들기 (1)" }));

    await waitFor(() => expect(liveChatApi.answerDraft).toHaveBeenCalledWith(5, ["m1"]));
    expect(await screen.findByDisplayValue("초안 본문")).toBeInTheDocument();

    await userEvent.click(screen.getByRole("button", { name: "답변으로 게시" }));

    await waitFor(() => expect(answerApi.create).toHaveBeenCalledWith(10, "초안 본문", { liveChatRoomId: 5 }));
    await waitFor(() => expect(push).toHaveBeenCalledWith("/questions/10#answer-77"));
  });

  it("shows the server's refusal when the viewer never took part in the room", async () => {
    vi.mocked(liveChatApi.answerDraft).mockRejectedValue(new ApiError(403, "FORBIDDEN", "질문방 참여자만 정리할 수 있습니다."));
    await renderPage();

    await userEvent.click(await screen.findByRole("checkbox", { name: /감사 로그만 JDBC예요/ }));
    await userEvent.click(screen.getByRole("button", { name: "선택한 메시지로 초안 만들기 (1)" }));

    expect(await screen.findByText("질문방 참여자만 정리할 수 있습니다.")).toBeInTheDocument();
    expect(answerApi.create).not.toHaveBeenCalled();
  });

  it("explains when the question has no live chat room", async () => {
    vi.mocked(liveChatApi.getRoom).mockRejectedValue(new ApiError(404, "NOT_FOUND", "no room"));
    await renderPage();

    expect(await screen.findByText("이 질문에는 아직 실시간 질문방이 없습니다.")).toBeInTheDocument();
  });
});
