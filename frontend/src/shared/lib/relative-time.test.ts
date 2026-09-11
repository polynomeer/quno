import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { relativeTime } from "./relative-time";

const NOW = new Date("2026-01-15T12:00:00.000Z");

describe("relativeTime", () => {
  beforeEach(() => {
    vi.useFakeTimers();
    vi.setSystemTime(NOW);
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it("returns '방금 전' for a past timestamp under a minute ago", () => {
    const thirtySecondsAgo = new Date(NOW.getTime() - 30 * 1000).toISOString();
    expect(relativeTime(thirtySecondsAgo)).toBe("방금 전");
  });

  it("formats a past timestamp in minutes", () => {
    const fiveMinutesAgo = new Date(NOW.getTime() - 5 * 60 * 1000).toISOString();
    expect(relativeTime(fiveMinutesAgo)).toBe("5분 전");
  });

  it("formats a past timestamp in days", () => {
    const threeDaysAgo = new Date(NOW.getTime() - 3 * 24 * 60 * 60 * 1000).toISOString();
    expect(relativeTime(threeDaysAgo)).toBe("3일 전");
  });

  it("formats a future timestamp", () => {
    const inTwoHours = new Date(NOW.getTime() + 2 * 60 * 60 * 1000).toISOString();
    expect(relativeTime(inTwoHours)).toBe("2시간 후");
  });

  it("returns '곧' for a future timestamp under a minute away", () => {
    const in30Seconds = new Date(NOW.getTime() + 30 * 1000).toISOString();
    expect(relativeTime(in30Seconds)).toBe("곧");
  });
});
