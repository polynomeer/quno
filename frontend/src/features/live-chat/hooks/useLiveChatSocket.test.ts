import { beforeEach, describe, expect, it, vi } from "vitest";
import { act, renderHook } from "@testing-library/react";
import { Client } from "@stomp/stompjs";
import { useLiveChatSocket } from "./useLiveChatSocket";
import { tokenStorage } from "@/shared/lib/token-storage";
import type { LiveChatMessage } from "../api/live-chat.types";

interface CapturedFrame {
  body: string;
}

vi.mock("@stomp/stompjs", () => {
  class MockClient {
    static instances: MockClient[] = [];
    onConnect?: () => void;
    onWebSocketClose?: () => void;
    beforeConnect?: () => void;
    subscriptions = new Map<string, (frame: CapturedFrame) => void>();
    activate = vi.fn();
    deactivate = vi.fn(() => {
      this.onWebSocketClose?.();
    });
    publish = vi.fn();
    subscribe = vi.fn((destination: string, callback: (frame: CapturedFrame) => void) => {
      this.subscriptions.set(destination, callback);
    });
    brokerURL: string;
    connectHeaders: Record<string, string>;

    constructor(options: {
      brokerURL: string;
      connectHeaders: Record<string, string>;
      onConnect?: () => void;
      onWebSocketClose?: () => void;
      beforeConnect?: () => void;
    }) {
      this.brokerURL = options.brokerURL;
      this.connectHeaders = options.connectHeaders;
      this.onConnect = options.onConnect;
      this.onWebSocketClose = options.onWebSocketClose;
      this.beforeConnect = options.beforeConnect;
      MockClient.instances.push(this);
    }

    simulateConnect() {
      this.beforeConnect?.();
      this.onConnect?.();
    }

    emit(destination: string, payload: unknown) {
      this.subscriptions.get(destination)?.({ body: JSON.stringify(payload) });
    }
  }
  return { Client: MockClient };
});

const MockClient = Client as unknown as {
  new (options: {
    brokerURL: string;
    connectHeaders: Record<string, string>;
    onConnect?: () => void;
    onWebSocketClose?: () => void;
    beforeConnect?: () => void;
  }): InstanceType<typeof Client> & {
    activate: ReturnType<typeof vi.fn>;
    deactivate: ReturnType<typeof vi.fn>;
    publish: ReturnType<typeof vi.fn>;
    connectHeaders: Record<string, string>;
    simulateConnect(): void;
    emit(destination: string, payload: unknown): void;
  };
  instances: Array<{
    activate: ReturnType<typeof vi.fn>;
    deactivate: ReturnType<typeof vi.fn>;
    publish: ReturnType<typeof vi.fn>;
    connectHeaders: Record<string, string>;
    simulateConnect(): void;
    emit(destination: string, payload: unknown): void;
  }>;
};

function message(overrides: Partial<LiveChatMessage> = {}): LiveChatMessage {
  return { id: "1", roomId: 10, senderId: 1, body: "안녕하세요", createdAt: "", ...overrides };
}

describe("useLiveChatSocket", () => {
  beforeEach(() => {
    MockClient.instances = [];
    tokenStorage.clear();
    tokenStorage.setTokens("access-token", "refresh-token");
  });

  it("does not create a connection when disabled", () => {
    renderHook(() => useLiveChatSocket(10, 1, false));
    expect(MockClient.instances).toHaveLength(0);
  });

  it("does not create a connection without a roomId", () => {
    renderHook(() => useLiveChatSocket(null, 1, true));
    expect(MockClient.instances).toHaveLength(0);
  });

  it("does not create a connection without an access token", () => {
    tokenStorage.clear();
    renderHook(() => useLiveChatSocket(10, 1, true));
    expect(MockClient.instances).toHaveLength(0);
  });

  it("activates a client with an Authorization header when enabled", () => {
    renderHook(() => useLiveChatSocket(10, 1, true));

    expect(MockClient.instances).toHaveLength(1);
    expect(MockClient.instances[0].connectHeaders).toEqual({ Authorization: "Bearer access-token" });
    expect(MockClient.instances[0].activate).toHaveBeenCalledTimes(1);
  });

  it("transitions to 'connected' once the STOMP connection succeeds", () => {
    const { result } = renderHook(() => useLiveChatSocket(10, 1, true));
    expect(result.current.status).toBe("disconnected");

    act(() => {
      MockClient.instances[0].simulateConnect();
    });

    expect(result.current.status).toBe("connected");
  });

  it("appends an incoming message from the room's message topic", () => {
    const { result } = renderHook(() => useLiveChatSocket(10, 5, true));
    act(() => {
      MockClient.instances[0].simulateConnect();
    });

    act(() => {
      MockClient.instances[0].emit("/topic/live-chat/10", message({ id: "m1", body: "hi" }));
    });

    expect(result.current.messages).toEqual([message({ id: "m1", body: "hi" })]);
  });

  it("does not add a duplicate message with the same id", () => {
    const { result } = renderHook(() => useLiveChatSocket(10, 5, true));
    act(() => {
      MockClient.instances[0].simulateConnect();
    });

    act(() => {
      MockClient.instances[0].emit("/topic/live-chat/10", message({ id: "m1" }));
      MockClient.instances[0].emit("/topic/live-chat/10", message({ id: "m1" }));
    });

    expect(result.current.messages).toHaveLength(1);
  });

  it("updates the viewer count from the presence topic", () => {
    const { result } = renderHook(() => useLiveChatSocket(10, 5, true));
    act(() => {
      MockClient.instances[0].simulateConnect();
    });

    act(() => {
      MockClient.instances[0].emit("/topic/questions/5/presence", { viewerCount: 3 });
    });

    expect(result.current.viewerCount).toBe(3);
  });

  it("does not send a message before the connection is established", () => {
    const { result } = renderHook(() => useLiveChatSocket(10, 5, true));

    act(() => {
      result.current.sendMessage("아직 연결 전");
    });

    expect(MockClient.instances[0].publish).not.toHaveBeenCalled();
  });

  it("publishes a message to the room's send destination once connected", () => {
    const { result } = renderHook(() => useLiveChatSocket(10, 5, true));
    act(() => {
      MockClient.instances[0].simulateConnect();
    });

    act(() => {
      result.current.sendMessage("안녕하세요");
    });

    expect(MockClient.instances[0].publish).toHaveBeenCalledWith({
      destination: "/app/live-chat/10/send",
      body: JSON.stringify({ body: "안녕하세요" }),
    });
  });

  it("deactivates the client and resets status on unmount", () => {
    const { result, unmount } = renderHook(() => useLiveChatSocket(10, 5, true));
    act(() => {
      MockClient.instances[0].simulateConnect();
    });
    expect(result.current.status).toBe("connected");

    unmount();

    expect(MockClient.instances[0].deactivate).toHaveBeenCalledTimes(1);
  });
});
