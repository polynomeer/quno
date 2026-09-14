import type { ReactElement } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { LiveChatPanel } from "./LiveChatPanel";
import { useSession } from "@/features/auth/hooks/useSession";
import { liveChatApi } from "../api/live-chat.api";
import { ApiError } from "@/shared/api/api-error";
import type { MyProfile } from "@/features/auth/api/auth.types";
import type { LiveChatRoom } from "../api/live-chat.types";

vi.mock("@/features/auth/hooks/useSession", () => ({ useSession: vi.fn() }));

vi.mock("../api/live-chat.api", () => ({
  liveChatApi: { getRoom: vi.fn(), openRoom: vi.fn(), messages: vi.fn() },
}));

// LiveChatSession owns the real STOMP connection and has its own dedicated test — stubbing it
// here keeps this file focused on LiveChatPanel's own room-open/join wiring, and avoids the
// dynamic import actually resolving @stomp/stompjs in every test.
vi.mock("./LiveChatSession", () => ({
  LiveChatSession: ({ roomId }: { roomId: number }) => <div data-testid="live-chat-session">room {roomId}</div>,
}));

const ME: MyProfile = { id: 1, email: "me@example.com", nickname: "me", acceptsDirectAsk: false, createdAt: "" };

const ROOM: LiveChatRoom = { id: 5, questionId: 10, createdBy: ME.id, createdAt: "" };

function renderWithClient(ui: ReactElement) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(<QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>);
}

describe("LiveChatPanel", () => {
  beforeEach(() => {
    vi.mocked(useSession).mockReturnValue({ data: ME } as ReturnType<typeof useSession>);
    vi.mocked(liveChatApi.getRoom).mockReset().mockRejectedValue(new ApiError(404, "NOT_FOUND", "no room"));
    vi.mocked(liveChatApi.openRoom).mockReset();
  });

  it("offers to start a room when none exists yet", async () => {
    renderWithClient(<LiveChatPanel questionId={10} />);
    expect(await screen.findByRole("button", { name: "실시간 질문방 시작하기" })).toBeInTheDocument();
  });

  it("offers to join an already-open room without starting a new one", async () => {
    vi.mocked(liveChatApi.getRoom).mockResolvedValue(ROOM);
    renderWithClient(<LiveChatPanel questionId={10} />);

    expect(await screen.findByRole("button", { name: "채팅 참여하기" })).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "실시간 질문방 시작하기" })).not.toBeInTheDocument();
  });

  it("opens a room and immediately joins it on success", async () => {
    vi.mocked(liveChatApi.openRoom).mockResolvedValue(ROOM);
    renderWithClient(<LiveChatPanel questionId={10} />);
    await userEvent.click(await screen.findByRole("button", { name: "실시간 질문방 시작하기" }));

    expect(await screen.findByTestId("live-chat-session")).toHaveTextContent("room 5");
  });

  it("joins an existing room when 채팅 참여하기 is clicked", async () => {
    vi.mocked(liveChatApi.getRoom).mockResolvedValue(ROOM);
    renderWithClient(<LiveChatPanel questionId={10} />);
    await userEvent.click(await screen.findByRole("button", { name: "채팅 참여하기" }));

    expect(await screen.findByTestId("live-chat-session")).toHaveTextContent("room 5");
  });

  it("shows the backend's error message when opening a room fails", async () => {
    vi.mocked(liveChatApi.openRoom).mockRejectedValue(new ApiError(403, "FORBIDDEN", "권한이 없습니다."));
    renderWithClient(<LiveChatPanel questionId={10} />);
    await userEvent.click(await screen.findByRole("button", { name: "실시간 질문방 시작하기" }));

    expect(await screen.findByText("권한이 없습니다.")).toBeInTheDocument();
    await waitFor(() => expect(screen.queryByTestId("live-chat-session")).not.toBeInTheDocument());
  });
});
