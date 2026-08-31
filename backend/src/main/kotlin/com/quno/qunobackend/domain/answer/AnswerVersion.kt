package com.quno.qunobackend.domain.answer

import java.time.Instant

/** Immutable, append-only revision of an Answer (Av1, Av2, ...) — mirrors QuestionVersion
 * (Phase 17, ADR-0029). Never updated after creation. */
class AnswerVersion private constructor(
    val id: Long?,
    val answerId: Long,
    val versionNumber: Int,
    val bodyMarkdown: String,
    val createdBy: Long,
    val createdAt: Instant,
) {
    companion object {
        fun create(answerId: Long, versionNumber: Int, bodyMarkdown: String, createdBy: Long): AnswerVersion {
            require(versionNumber >= 1) { "versionNumber must be >= 1" }
            require(bodyMarkdown.isNotBlank()) { "bodyMarkdown must not be blank" }
            return AnswerVersion(
                id = null,
                answerId = answerId,
                versionNumber = versionNumber,
                bodyMarkdown = bodyMarkdown,
                createdBy = createdBy,
                createdAt = Instant.now(),
            )
        }

        fun reconstitute(
            id: Long,
            answerId: Long,
            versionNumber: Int,
            bodyMarkdown: String,
            createdBy: Long,
            createdAt: Instant,
        ): AnswerVersion = AnswerVersion(id, answerId, versionNumber, bodyMarkdown, createdBy, createdAt)
    }
}
