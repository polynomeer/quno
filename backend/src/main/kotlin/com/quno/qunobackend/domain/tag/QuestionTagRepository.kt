package com.quno.qunobackend.domain.tag

/**
 * Port for the question_tags relation table. Pure relation data (hard delete
 * allowed) — see docs/architecture/domain-model.md 삭제/FK 운영 원칙.
 */
interface QuestionTagRepository {
    /** Idempotent: attaching an already-attached tag is a no-op. */
    fun attach(questionId: Long, tagId: Long)
    fun findTagsByQuestionId(questionId: Long): List<Tag>
}
