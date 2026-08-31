package com.quno.qunobackend.domain.answer

import java.time.Instant

/** Independent Aggregate — a Question can accumulate many answers without bloating Question itself. */
class Answer private constructor(
    val id: Long?,
    val questionId: Long,
    val authorId: Long,
    val bodyMarkdown: String,
    val isAccepted: Boolean,
    val targetVersionNumber: Int,
    val deletedAt: Instant?,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    fun accept(): Answer {
        check(deletedAt == null) { "cannot accept a deleted answer" }
        return Answer(id, questionId, authorId, bodyMarkdown, true, targetVersionNumber, deletedAt, createdAt, Instant.now())
    }

    fun unaccept(): Answer =
        Answer(id, questionId, authorId, bodyMarkdown, false, targetVersionNumber, deletedAt, createdAt, Instant.now())

    /** Used by moderation Hide (Phase 16, ADR-0028). Idempotent. */
    fun softDelete(): Answer {
        if (deletedAt != null) return this
        return Answer(id, questionId, authorId, bodyMarkdown, isAccepted, targetVersionNumber, Instant.now(), createdAt, Instant.now())
    }

    companion object {
        /** [targetVersionNumber] is the question's latest version number at write time (see PLAN.md 5.1). */
        fun write(questionId: Long, authorId: Long, bodyMarkdown: String, targetVersionNumber: Int): Answer {
            require(bodyMarkdown.isNotBlank()) { "bodyMarkdown must not be blank" }
            require(targetVersionNumber >= 1) { "targetVersionNumber must be >= 1" }
            val now = Instant.now()
            return Answer(
                id = null,
                questionId = questionId,
                authorId = authorId,
                bodyMarkdown = bodyMarkdown,
                isAccepted = false,
                targetVersionNumber = targetVersionNumber,
                deletedAt = null,
                createdAt = now,
                updatedAt = now,
            )
        }

        fun reconstitute(
            id: Long,
            questionId: Long,
            authorId: Long,
            bodyMarkdown: String,
            isAccepted: Boolean,
            targetVersionNumber: Int,
            deletedAt: Instant?,
            createdAt: Instant,
            updatedAt: Instant,
        ): Answer = Answer(id, questionId, authorId, bodyMarkdown, isAccepted, targetVersionNumber, deletedAt, createdAt, updatedAt)
    }
}
