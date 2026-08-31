package com.quno.qunobackend.domain.answer

import java.time.Instant

/**
 * Independent Aggregate — a Question can accumulate many answers without bloating Question itself.
 * `bodyMarkdown` is a cache of the latest [AnswerVersion]'s content (Phase 17, ADR-0029) — the same
 * pattern `questions.title` uses to cache the latest QuestionVersion's title. `latestVersionId` is
 * null only in the brief window between inserting the Answer row and inserting its first version.
 */
class Answer private constructor(
    val id: Long?,
    val questionId: Long,
    val authorId: Long,
    val bodyMarkdown: String,
    val isAccepted: Boolean,
    val targetVersionNumber: Int,
    val latestVersionId: Long?,
    val deletedAt: Instant?,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    fun accept(): Answer {
        check(deletedAt == null) { "cannot accept a deleted answer" }
        return Answer(id, questionId, authorId, bodyMarkdown, true, targetVersionNumber, latestVersionId, deletedAt, createdAt, Instant.now())
    }

    fun unaccept(): Answer =
        Answer(id, questionId, authorId, bodyMarkdown, false, targetVersionNumber, latestVersionId, deletedAt, createdAt, Instant.now())

    /** Used by moderation Hide (Phase 16, ADR-0028). Idempotent. */
    fun softDelete(): Answer {
        if (deletedAt != null) return this
        return Answer(id, questionId, authorId, bodyMarkdown, isAccepted, targetVersionNumber, latestVersionId, Instant.now(), createdAt, Instant.now())
    }

    /** Used right after inserting the first AnswerVersion: wires the pointer and refreshes the body cache. */
    fun withLatestVersion(versionId: Long, bodyMarkdown: String = this.bodyMarkdown): Answer =
        Answer(id, questionId, authorId, bodyMarkdown, isAccepted, targetVersionNumber, versionId, deletedAt, createdAt, Instant.now())

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
                latestVersionId = null,
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
            latestVersionId: Long?,
            deletedAt: Instant?,
            createdAt: Instant,
            updatedAt: Instant,
        ): Answer = Answer(
            id, questionId, authorId, bodyMarkdown, isAccepted, targetVersionNumber, latestVersionId, deletedAt, createdAt, updatedAt,
        )
    }
}
