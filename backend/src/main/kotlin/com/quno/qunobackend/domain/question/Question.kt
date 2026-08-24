package com.quno.qunobackend.domain.question

import java.time.Instant

/**
 * Aggregate root. QuestionVersion content is intentionally not held here as a
 * collection (see docs/architecture/system-architecture.md#question-aggregate-설계-원칙) —
 * only the latest version's id is tracked, so revisions can accumulate without
 * bloating this aggregate.
 */
class Question private constructor(
    val id: Long?,
    val authorId: Long,
    val title: String,
    val status: QuestionStatus,
    val latestVersionId: Long?,
    val acceptedAnswerId: Long?,
    val deletedAt: Instant?,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    fun withLatestVersion(versionId: Long, title: String = this.title): Question =
        Question(id, authorId, title, status, versionId, acceptedAnswerId, deletedAt, createdAt, Instant.now())

    companion object {
        fun open(authorId: Long, title: String): Question {
            require(title.isNotBlank()) { "title must not be blank" }
            val now = Instant.now()
            return Question(
                id = null,
                authorId = authorId,
                title = title,
                status = QuestionStatus.OPEN,
                latestVersionId = null,
                acceptedAnswerId = null,
                deletedAt = null,
                createdAt = now,
                updatedAt = now,
            )
        }

        fun reconstitute(
            id: Long,
            authorId: Long,
            title: String,
            status: QuestionStatus,
            latestVersionId: Long?,
            acceptedAnswerId: Long?,
            deletedAt: Instant?,
            createdAt: Instant,
            updatedAt: Instant,
        ): Question = Question(
            id, authorId, title, status, latestVersionId, acceptedAnswerId, deletedAt, createdAt, updatedAt,
        )
    }
}
