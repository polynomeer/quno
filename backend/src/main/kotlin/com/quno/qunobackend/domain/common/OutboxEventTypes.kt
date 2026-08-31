package com.quno.qunobackend.domain.common

/** Event type constants — matches the Ward notification triggers in docs/product/mvp-scope.md. */
object OutboxEventTypes {
    const val QUESTION_REVISION = "QUESTION_REVISION"
    const val NEW_ANSWER = "NEW_ANSWER"
    const val ANSWER_ACCEPTED = "ANSWER_ACCEPTED"
    const val REVIEW_REQUESTED = "REVIEW_REQUESTED"
    const val REVIEW_RE_REQUESTED = "REVIEW_RE_REQUESTED"
    const val QUESTION_OUTDATED = "QUESTION_OUTDATED"
    const val NEW_COMMENT = "NEW_COMMENT"
    const val CONTENT_HIDDEN = "CONTENT_HIDDEN"
    const val ANSWER_REVISION = "ANSWER_REVISION"
    const val MENTIONED_IN_COMMENT = "MENTIONED_IN_COMMENT"
}
