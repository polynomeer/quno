import { beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { LiveChatSession } from "./LiveChatSession";
import { useLiveChatMessageHistory } from "../hooks/useLiveChatMessageHistory";
import { useLiveChatSocket } from "../hooks/useLiveChatSocket";
import type { LiveChatMessage } from "../api/live-chat.types";

vi.mock("../hooks/useLiveChatMessageHistory", () => ({ useLiveChatMessageHistory: vi.fn() }));
vi.mock("../hooks/useLiveChatSocket", () => ({ useLiveChatSocket: vi.fn() }));

function chatMessage(overrides: Partial<LiveChatMessage> = {}): LiveChatMessage {
  return { id: "1", roomId: 5, senderId: 1, body: "안녕하세요", createdAt: "2026-01-01T00:00:00.000Z", ...overrides };
}

function mockSocket(overrides: Partial<ReturnType<typeof useLiveChatSocket>> = {}) {
  vi.mocked(useLiveChatSocket).mockReturnValue({
    messages: [],
    viewerCount: null,
    status: "connected",
    sendMessage: vi.fn(),
    ...overrides,
  });
}

describe("LiveChatSession", () => {
  beforeEach(() => {
    vi.mocked(useLiveChatMessageHistory).mockReturnValue({ data: [] } as unknown as ReturnType<typeof useLiveChatMessageHistory>);
    mockSocket();
  });

  it("shows 'connecting' placeholder text while not yet connected and there are no messages", () => {
    mockSocket({ status: "connecting" });
    render(<LiveChatSession roomId={5} questionId={10} myUserId={1} />);
    expect(screen.getByText("연결 중...")).toBeInTheDocument();
  });

  it("shows the empty-chat placeholder once connected with no messages", () => {
    mockSocket({ status: "connected" });
    render(<LiveChatSession roomId={5} questionId={10} myUserId={1} />);
    expect(screen.getByText("아직 메시지가 없습니다.")).toBeInTheDocument();
  });

  it("shows the viewer count only when it's known", () => {
    const { rerender } = render(<LiveChatSession roomId={5} questionId={10} myUserId={1} />);
    expect(screen.queryByText(/명이 보고 있습니다/)).not.toBeInTheDocument();

    mockSocket({ viewerCount: 4 });
    rerender(<LiveChatSession roomId={5} questionId={10} myUserId={1} />);
    expect(screen.getByText("현재 4명이 보고 있습니다")).toBeInTheDocument();
  });

  it("merges history and live messages, deduplicating by id", () => {
    vi.mocked(useLiveChatMessageHistory).mockReturnValue({
      data: [chatMessage({ id: "h1", body: "이전 메시지" })],
    } as unknown as ReturnType<typeof useLiveChatMessageHistory>);
    mockSocket({ messages: [chatMessage({ id: "h1", body: "이전 메시지" }), chatMessage({ id: "l1", body: "실시간 메시지" })] });

    render(<LiveChatSession roomId={5} questionId={10} myUserId={1} />);

    expect(screen.getAllByText("이전 메시지")).toHaveLength(1);
    expect(screen.getByText("실시간 메시지")).toBeInTheDocument();
  });

  it("labels the viewer's own messages as '나' and others by user id", () => {
    mockSocket({ messages: [chatMessage({ id: "1", senderId: 1 }), chatMessage({ id: "2", senderId: 2 })] });
    render(<LiveChatSession roomId={5} questionId={10} myUserId={1} />);

    expect(screen.getByRole("link", { name: "나" })).toHaveAttribute("href", "/users/1");
    expect(screen.getByRole("link", { name: "사용자 #2" })).toHaveAttribute("href", "/users/2");
  });

  it("disables the input and send button while not connected", () => {
    mockSocket({ status: "connecting" });
    render(<LiveChatSession roomId={5} questionId={10} myUserId={1} />);

    expect(screen.getByPlaceholderText("연결 중...")).toBeDisabled();
    expect(screen.getByRole("button", { name: "보내기" })).toBeDisabled();
  });

  it("keeps 보내기 disabled until a message is typed", async () => {
    render(<LiveChatSession roomId={5} questionId={10} myUserId={1} />);
    expect(screen.getByRole("button", { name: "보내기" })).toBeDisabled();

    await userEvent.type(screen.getByPlaceholderText("메시지 입력..."), "안녕");

    expect(screen.getByRole("button", { name: "보내기" })).toBeEnabled();
  });

  it("sends the typed message and clears the input when 보내기 is clicked", async () => {
    const sendMessage = vi.fn();
    mockSocket({ sendMessage });
    render(<LiveChatSession roomId={5} questionId={10} myUserId={1} />);
    const input = screen.getByPlaceholderText("메시지 입력...");
    await userEvent.type(input, "안녕하세요");

    await userEvent.click(screen.getByRole("button", { name: "보내기" }));

    expect(sendMessage).toHaveBeenCalledWith("안녕하세요");
    expect(input).toHaveValue("");
  });

  it("sends the message on Enter without inserting a newline", async () => {
    const sendMessage = vi.fn();
    mockSocket({ sendMessage });
    render(<LiveChatSession roomId={5} questionId={10} myUserId={1} />);

    await userEvent.type(screen.getByPlaceholderText("메시지 입력..."), "안녕하세요{Enter}");

    expect(sendMessage).toHaveBeenCalledWith("안녕하세요");
  });
});
