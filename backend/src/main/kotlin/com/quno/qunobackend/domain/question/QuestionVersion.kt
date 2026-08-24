package com.quno.qunobackend.domain.question

import java.time.Instant

/** Immutable, append-only revision of a Question (Qv1, Qv2, ...). Never updated after creation. */
class QuestionVersion private constructor(
    val id: Long?,
    val questionId: Long,
    val versionNumber: Int,
    val title: String,
    val bodyMarkdown: String,
    val environment: String?,
    val logs: String?,
    val createdBy: Long,
    val createdAt: Instant,
) {
    companion object {
        fun create(
            questionId: Long,
            versionNumber: Int,
            title: String,
            bodyMarkdown: String,
            environment: String?,
            logs: String?,
            createdBy: Long,
        ): QuestionVersion {
            require(versionNumber >= 1) { "versionNumber must be >= 1" }
            require(title.isNotBlank()) { "title must not be blank" }
            require(bodyMarkdown.isNotBlank()) { "bodyMarkdown must not be blank" }
            return QuestionVersion(
                id = null,
                questionId = questionId,
                versionNumber = versionNumber,
                title = title,
                bodyMarkdown = bodyMarkdown,
                environment = environment,
                logs = logs,
                createdBy = createdBy,
                createdAt = Instant.now(),
            )
        }

        fun reconstitute(
            id: Long,
            questionId: Long,
            versionNumber: Int,
            title: String,
            bodyMarkdown: String,
            environment: String?,
            logs: String?,
            createdBy: Long,
            createdAt: Instant,
        ): QuestionVersion = QuestionVersion(
            id, questionId, versionNumber, title, bodyMarkdown, environment, logs, createdBy, createdAt,
        )
    }
}
