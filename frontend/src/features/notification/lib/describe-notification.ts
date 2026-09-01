import type { Notification } from "../api/notification.types";

interface DescribedNotification {
  message: string;
  href: string;
}

function safeParsePayload(payload: string): Record<string, unknown> {
  try {
    return JSON.parse(payload) as Record<string, unknown>;
  } catch {
    return {};
  }
}

/**
 * The backend has no per-notification "rendered message" — each type's `payload` is a raw JSON
 * string with different fields (see the OutboxEvent.create call sites this mirrors:QuestionRevision,
 * WriteAnswerUseCase, AcceptAnswerUseCase, CreateReviewRequestUseCase, ReRequestReviewUseCase,
 * MarkQuestionOutdatedUseCase). This is where that gets translated into something readable.
 */
export function describeNotification(notification: Notification): DescribedNotification {
  const payload = safeParsePayload(notification.payload);
  const questionHref = notification.questionId ? `/questions/${notification.questionId}` : "/";
  const href = notification.answerId ? `${questionHref}#answer-${notification.answerId}` : questionHref;

  switch (notification.type) {
    case "QUESTION_REVISION":
      return { message: `질문이 수정되었습니다 (버전 ${String(payload.versionNumber ?? "?")})`, href };
    case "NEW_ANSWER":
      return { message: "새 답변이 달렸습니다", href };
    case "ANSWER_ACCEPTED":
      return { message: "답변이 채택되었습니다", href };
    case "REVIEW_REQUESTED":
      return { message: "정보 요청이 도착했습니다", href };
    case "REVIEW_RE_REQUESTED":
      return { message: "재요청이 처리되었습니다", href };
    case "QUESTION_OUTDATED": {
      const reason = typeof payload.reason === "string" ? payload.reason : null;
      return { message: `질문이 Outdated로 표시되었습니다${reason ? ` — ${reason}` : ""}`, href };
    }
    case "NEW_COMMENT":
      return { message: "새 댓글이 달렸습니다", href };
    case "ANSWER_REVISION":
      return { message: "답변이 수정되었습니다", href };
    case "CONTENT_HIDDEN":
      return { message: "모더레이터에 의해 콘텐츠가 숨겨졌습니다", href };
    case "MENTIONED_IN_COMMENT":
      return { message: "댓글에서 언급되었습니다", href };
    case "TECH_VERSION_IMPACT_DETECTED": {
      const tagSlug = typeof payload.tagSlug === "string" ? payload.tagSlug : null;
      const latestVersion = typeof payload.latestVersion === "string" ? payload.latestVersion : null;
      return {
        message: `${tagSlug ?? "관련 기술"}${latestVersion ? ` ${latestVersion}` : ""} 릴리스로 이 질문이 오래되었을 수 있습니다`,
        href,
      };
    }
    default:
      return { message: notification.type, href };
  }
}
