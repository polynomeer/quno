package com.quno.qunobackend.domain.common

/** Event type constants — matches the Ward notification triggers in docs/product/mvp-scope.md. */
object OutboxEventTypes {
    const val QUESTION_REVISION = "QUESTION_REVISION"
    const val NEW_ANSWER = "NEW_ANSWER"
    const val ANSWER_ACCEPTED = "ANSWER_ACCEPTED"
}
