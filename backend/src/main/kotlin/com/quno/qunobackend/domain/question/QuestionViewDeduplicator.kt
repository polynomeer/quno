package com.quno.qunobackend.domain.question

/**
 * Decides whether a view should count (ADR-0063): the same viewer re-opening the same question
 * within the dedup window is one view, not many. Implemented by
 * infrastructure/question/RedisQuestionViewDeduplicator.
 */
interface QuestionViewDeduplicator {
    /** True (and remembers it) the first time [viewerKey] views [questionId] within the window. */
    fun isFirstView(questionId: Long, viewerKey: String): Boolean
}
