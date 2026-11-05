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
    const val TECH_VERSION_IMPACT_DETECTED = "TECH_VERSION_IMPACT_DETECTED"
    const val DIRECT_ASK_REQUESTED = "DIRECT_ASK_REQUESTED"
    const val DIRECT_ASK_ACCEPTED = "DIRECT_ASK_ACCEPTED"
    const val DIRECT_ASK_DECLINED = "DIRECT_ASK_DECLINED"
    const val LIVE_CHAT_STARTED = "LIVE_CHAT_STARTED"

    /** A new release changed how a question's reproduction behaves (Phase 82, ADR-0090). */
    const val RELEASE_REPRODUCTION_CHANGED = "RELEASE_REPRODUCTION_CHANGED"

    /** Author-only (ADR-0091): a search they started has an answer for them to judge. */
    const val VERSION_SEARCH_FINISHED = "VERSION_SEARCH_FINISHED"

    /** Author-only (ADR-0081): watchers must not learn where a secret sits. */
    const val ENGINE_SECRET_DETECTED = "ENGINE_SECRET_DETECTED"
}
