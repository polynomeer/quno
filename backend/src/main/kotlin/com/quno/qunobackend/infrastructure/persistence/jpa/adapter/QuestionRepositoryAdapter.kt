package com.quno.qunobackend.infrastructure.persistence.jpa.adapter

import com.quno.qunobackend.domain.question.Question
import com.quno.qunobackend.domain.question.QuestionRepository
import com.quno.qunobackend.infrastructure.persistence.jpa.entity.QuestionJpaEntity
import com.quno.qunobackend.infrastructure.persistence.jpa.repository.QuestionJpaRepository
import org.springframework.stereotype.Component

@Component
class QuestionRepositoryAdapter(
    private val jpaRepository: QuestionJpaRepository,
) : QuestionRepository {

    override fun save(question: Question): Question {
        val entity = QuestionJpaEntity(
            id = question.id,
            authorId = question.authorId,
            title = question.title,
            status = question.status,
            latestVersionId = question.latestVersionId,
            acceptedAnswerId = question.acceptedAnswerId,
            clusterId = question.clusterId,
            originQuestionId = question.originQuestionId,
            deletedAt = question.deletedAt,
            createdAt = question.createdAt,
            updatedAt = question.updatedAt,
        )
        return jpaRepository.save(entity).toDomain()
    }

    override fun findById(id: Long): Question? = jpaRepository.findByIdAndDeletedAtIsNull(id)?.toDomain()

    override fun findByIdForUpdate(id: Long): Question? = jpaRepository.findByIdForUpdate(id)?.toDomain()

    override fun findAllByAuthorId(authorId: Long): List<Question> =
        jpaRepository.findAllByAuthorIdAndDeletedAtIsNullOrderByCreatedAtDesc(authorId).map { it.toDomain() }

    override fun findAllByClusterId(clusterId: Long): List<Question> =
        jpaRepository.findAllByClusterIdAndDeletedAtIsNull(clusterId).map { it.toDomain() }

    override fun findAllByOriginQuestionId(originQuestionId: Long): List<Question> =
        jpaRepository.findAllByOriginQuestionIdAndDeletedAtIsNull(originQuestionId).map { it.toDomain() }

    private fun QuestionJpaEntity.toDomain(): Question = Question.reconstitute(
        id = requireNotNull(id),
        authorId = authorId,
        title = title,
        status = status,
        latestVersionId = latestVersionId,
        acceptedAnswerId = acceptedAnswerId,
        clusterId = clusterId,
        originQuestionId = originQuestionId,
        deletedAt = deletedAt,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
