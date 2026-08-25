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
    /** Used right after creating Qv1: keeps status OPEN, only wires the pointer. */
    fun withLatestVersion(versionId: Long, title: String = this.title): Question =
        Question(id, authorId, title, status, versionId, acceptedAnswerId, deletedAt, createdAt, Instant.now())

    /**
     * Used when a revision (Qv2+) is appended. A RESOLVED question stays RESOLVED —
     * MVP has no re-open flow yet (see docs/product/mvp-scope.md 로드맵).
     */
    fun revise(versionId: Long, title: String): Question {
        val newStatus = if (status == QuestionStatus.RESOLVED) status else QuestionStatus.UPDATED
        return Question(id, authorId, title, newStatus, versionId, acceptedAnswerId, deletedAt, createdAt, Instant.now())
    }

    /** Used when an answer is accepted. */
    fun resolve(acceptedAnswerId: Long): Question =
        Question(id, authorId, title, QuestionStatus.RESOLVED, latestVersionId, acceptedAnswerId, deletedAt, createdAt, Instant.now())

    /**
     * Used when a reviewer opens a ReviewRequest (QPR "Review", PLAN.md 5.2). A RESOLVED
     * question can't be sent back to NEEDS_INFO — MVP has no re-open flow yet. Already being
     * NEEDS_INFO (another open request exists) is a no-op, since multiple reviewers can have
     * independent open requests at once.
     */
    fun requestMoreInfo(): Question {
        if (status == QuestionStatus.RESOLVED) throw QuestionAlreadyResolvedException(requireNotNull(id))
        if (status == QuestionStatus.NEEDS_INFO) return this
        return Question(id, authorId, title, QuestionStatus.NEEDS_INFO, latestVersionId, acceptedAnswerId, deletedAt, createdAt, Instant.now())
    }

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
