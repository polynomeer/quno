package com.quno.qunobackend.domain.answer

import java.time.Instant

/** Independent Aggregate — a Question can accumulate many answers without bloating Question itself. */
class Answer private constructor(
    val id: Long?,
    val questionId: Long,
    val authorId: Long,
    val bodyMarkdown: String,
    val isAccepted: Boolean,
    val deletedAt: Instant?,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    fun accept(): Answer {
        check(deletedAt == null) { "cannot accept a deleted answer" }
        return Answer(id, questionId, authorId, bodyMarkdown, true, deletedAt, createdAt, Instant.now())
    }

    fun unaccept(): Answer =
        Answer(id, questionId, authorId, bodyMarkdown, false, deletedAt, createdAt, Instant.now())

    companion object {
        fun write(questionId: Long, authorId: Long, bodyMarkdown: String): Answer {
            require(bodyMarkdown.isNotBlank()) { "bodyMarkdown must not be blank" }
            val now = Instant.now()
            return Answer(
                id = null,
                questionId = questionId,
                authorId = authorId,
                bodyMarkdown = bodyMarkdown,
                isAccepted = false,
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
            deletedAt: Instant?,
            createdAt: Instant,
            updatedAt: Instant,
        ): Answer = Answer(id, questionId, authorId, bodyMarkdown, isAccepted, deletedAt, createdAt, updatedAt)
    }
}
