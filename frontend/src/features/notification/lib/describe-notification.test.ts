import { describe, expect, it } from "vitest";
import { describeNotification } from "./describe-notification";
import type { Notification } from "../api/notification.types";

describe("describeNotification", () => {
  it("points a secret detection at the QunoBot card", () => {
    const notification = {
      id: 1,
      type: "ENGINE_SECRET_DETECTED",
      questionId: 10,
      answerId: null,
      payload: '{"questionAuthorId":1,"suggestionId":5,"count":2}',
      isRead: false,
      createdAt: "",
    } as unknown as Notification;

    expect(describeNotification(notification)).toEqual({
      message: "QunoBot이 질문에서 비밀값으로 보이는 문자열 2개를 찾아 공개 화면에서 가렸습니다",
      href: "/questions/10#qunobot",
    });
  });

  it("says which way a release changed the reproduction", () => {
    const notification = (transition: string) =>
      ({
        id: 2,
        type: "RELEASE_REPRODUCTION_CHANGED",
        questionId: 10,
        answerId: null,
        payload: JSON.stringify({ transition, fromVersion: "3.7.0", toVersion: "3.8.0" }),
        isRead: false,
        createdAt: "",
      }) as unknown as Notification;

    expect(describeNotification(notification("BROKE")).message).toBe("새 릴리스 3.8.0에서 이 문제가 재현됩니다. 답변을 다시 확인해 주세요");
    expect(describeNotification(notification("FIXED")).message).toBe("새 릴리스 3.8.0에서 이 문제가 재현되지 않습니다. 수정됐을 수 있어요");
  });
});
