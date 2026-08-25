package com.quno.qunobackend.infrastructure.persistence.jpa.adapter

import com.quno.qunobackend.domain.answer.Answer
import com.quno.qunobackend.domain.answer.AnswerRepository
import com.quno.qunobackend.infrastructure.persistence.jpa.entity.AnswerJpaEntity
import com.quno.qunobackend.infrastructure.persistence.jpa.repository.AnswerJpaRepository
import org.springframework.stereotype.Component

@Component
class AnswerRepositoryAdapter(
    private val jpaRepository: AnswerJpaRepository,
) : AnswerRepository {

    override fun save(answer: Answer): Answer {
        val entity = AnswerJpaEntity(
            id = answer.id,
            questionId = answer.questionId,
            authorId = answer.authorId,
            bodyMarkdown = answer.bodyMarkdown,
            isAccepted = answer.isAccepted,
            targetVersionNumber = answer.targetVersionNumber,
            deletedAt = answer.deletedAt,
            createdAt = answer.createdAt,
            updatedAt = answer.updatedAt,
        )
        return jpaRepository.save(entity).toDomain()
    }

    override fun findById(id: Long): Answer? = jpaRepository.findByIdAndDeletedAtIsNull(id)?.toDomain()

    override fun findAllByQuestionId(questionId: Long): List<Answer> =
        jpaRepository.findAllByQuestionIdAndDeletedAtIsNull(questionId).map { it.toDomain() }

    override fun findAcceptedByQuestionId(questionId: Long): Answer? =
        jpaRepository.findByQuestionIdAndIsAcceptedTrueAndDeletedAtIsNull(questionId)?.toDomain()

    override fun findAllByAuthorId(authorId: Long): List<Answer> =
        jpaRepository.findAllByAuthorIdAndDeletedAtIsNullOrderByCreatedAtDesc(authorId).map { it.toDomain() }

    private fun AnswerJpaEntity.toDomain(): Answer = Answer.reconstitute(
        id = requireNotNull(id),
        questionId = questionId,
        authorId = authorId,
        bodyMarkdown = bodyMarkdown,
        isAccepted = isAccepted,
        targetVersionNumber = targetVersionNumber,
        deletedAt = deletedAt,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
