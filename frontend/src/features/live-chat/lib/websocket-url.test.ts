import { afterEach, describe, expect, it } from "vitest";
import { getLiveChatWebSocketUrl } from "./websocket-url";

const ORIGINAL = process.env.NEXT_PUBLIC_API_BASE_URL;

describe("getLiveChatWebSocketUrl", () => {
  afterEach(() => {
    process.env.NEXT_PUBLIC_API_BASE_URL = ORIGINAL;
  });

  it("defaults to ws://localhost:8081/ws when no API base URL is configured", () => {
    delete process.env.NEXT_PUBLIC_API_BASE_URL;
    expect(getLiveChatWebSocketUrl()).toBe("ws://localhost:8081/ws");
  });

  it("converts an http:// API base URL to ws://", () => {
    process.env.NEXT_PUBLIC_API_BASE_URL = "http://api.quno.dev";
    expect(getLiveChatWebSocketUrl()).toBe("ws://api.quno.dev/ws");
  });

  it("converts an https:// API base URL to wss://", () => {
    process.env.NEXT_PUBLIC_API_BASE_URL = "https://api.quno.dev";
    expect(getLiveChatWebSocketUrl()).toBe("wss://api.quno.dev/ws");
  });
});
