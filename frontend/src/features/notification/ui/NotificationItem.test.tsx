import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { NotificationItem } from "./NotificationItem";
import type { Notification, NotificationType } from "../api/notification.types";

function notification(overrides: Partial<Notification> = {}): Notification {
  return {
    id: 1,
    type: "NEW_ANSWER",
    questionId: 10,
    answerId: null,
    payload: "{}",
    isRead: true,
    createdAt: "2026-01-01T00:00:00.000Z",
    ...overrides,
  };
}

describe("NotificationItem", () => {
  it("shows an unread dot and highlighted border only when unread", () => {
    const { rerender } = render(
      <ul>
        <NotificationItem notification={notification({ isRead: false })} />
      </ul>,
    );
    expect(screen.getByLabelText("읽지 않음")).toBeInTheDocument();

    rerender(
      <ul>
        <NotificationItem notification={notification({ isRead: true })} />
      </ul>,
    );
    expect(screen.queryByLabelText("읽지 않음")).not.toBeInTheDocument();
  });

  it("links to the question when there's no answer", () => {
    render(
      <ul>
        <NotificationItem notification={notification({ questionId: 10, answerId: null })} />
      </ul>,
    );
    expect(screen.getByRole("link")).toHaveAttribute("href", "/questions/10");
  });

  it("links to the answer anchor within the question when there is one", () => {
    render(
      <ul>
        <NotificationItem notification={notification({ questionId: 10, answerId: 5 })} />
      </ul>,
    );
    expect(screen.getByRole("link")).toHaveAttribute("href", "/questions/10#answer-5");
  });

  it.each([
    ["NEW_ANSWER", "{}", "새 답변이 달렸습니다"],
    ["ANSWER_ACCEPTED", "{}", "답변이 채택되었습니다"],
    ["NEW_COMMENT", "{}", "새 댓글이 달렸습니다"],
    ["CONTENT_HIDDEN", "{}", "모더레이터에 의해 콘텐츠가 숨겨졌습니다"],
    ["QUESTION_REVISION", '{"versionNumber":3}', "질문이 수정되었습니다 (버전 3)"],
    ["QUESTION_OUTDATED", '{"reason":"더 이상 재현되지 않음"}', "질문이 Outdated로 표시되었습니다 — 더 이상 재현되지 않음"],
    ["QUESTION_OUTDATED", "{}", "질문이 Outdated로 표시되었습니다"],
  ] as [NotificationType, string, string][])("renders the %s message correctly", (type, payload, expected) => {
    render(
      <ul>
        <NotificationItem notification={notification({ type, payload })} />
      </ul>,
    );
    expect(screen.getByText(expected)).toBeInTheDocument();
  });

  it("routes Direct Ask notifications to the direct-asks list instead of the question", () => {
    render(
      <ul>
        <NotificationItem notification={notification({ type: "DIRECT_ASK_REQUESTED", questionId: null })} />
      </ul>,
    );
    expect(screen.getByRole("link")).toHaveAttribute("href", "/direct-asks?role=received");
  });

  it("falls back to the raw type string for an unmapped type", () => {
    render(
      <ul>
        <NotificationItem notification={notification({ type: "UNKNOWN_FUTURE_TYPE" as NotificationType })} />
      </ul>,
    );
    expect(screen.getByText("UNKNOWN_FUTURE_TYPE")).toBeInTheDocument();
  });

  it("does not throw on malformed JSON payload", () => {
    render(
      <ul>
        <NotificationItem notification={notification({ type: "QUESTION_REVISION", payload: "not json" })} />
      </ul>,
    );
    expect(screen.getByText("질문이 수정되었습니다 (버전 ?)")).toBeInTheDocument();
  });
});
