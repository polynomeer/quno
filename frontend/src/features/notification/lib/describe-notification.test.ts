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
});
