import { describe, expect, it } from "vitest";
import { notificationCategory } from "./notification-category";

describe("notificationCategory", () => {
  it.each([
    ["NEW_ANSWER", "answer"],
    ["LIVE_CHAT_STARTED", "answer"],
    ["MENTIONED_IN_COMMENT", "conversation"],
    ["DIRECT_ASK_REQUESTED", "conversation"],
    ["QUESTION_OUTDATED", "change"],
  ])("puts %s under %s", (type, category) => {
    expect(notificationCategory(type)).toBe(category);
  });

  it("falls back to change for a type it doesn't know yet", () => {
    expect(notificationCategory("UNKNOWN_FUTURE_TYPE")).toBe("change");
  });
});
