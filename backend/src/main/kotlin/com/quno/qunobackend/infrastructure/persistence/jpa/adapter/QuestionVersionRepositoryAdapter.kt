package com.quno.qunobackend.infrastructure.persistence.jpa.adapter

import com.quno.qunobackend.domain.question.QuestionVersion
import com.quno.qunobackend.domain.question.QuestionVersionRepository
import com.quno.qunobackend.infrastructure.persistence.jpa.entity.QuestionVersionJpaEntity
import com.quno.qunobackend.infrastructure.persistence.jpa.repository.QuestionVersionJpaRepository
import org.springframework.stereotype.Component

@Component
class QuestionVersionRepositoryAdapter(
    private val jpaRepository: QuestionVersionJpaRepository,
) : QuestionVersionRepository {

    override fun save(version: QuestionVersion): QuestionVersion {
        val entity = QuestionVersionJpaEntity(
            id = version.id,
            questionId = version.questionId,
            versionNumber = version.versionNumber,
            title = version.title,
            bodyMarkdown = version.bodyMarkdown,
            environment = version.environment,
            logs = version.logs,
            createdBy = version.createdBy,
            createdAt = version.createdAt,
        )
        return jpaRepository.save(entity).toDomain()
    }

    override fun findById(id: Long): QuestionVersion? = jpaRepository.findById(id).orElse(null)?.toDomain()

    private fun QuestionVersionJpaEntity.toDomain(): QuestionVersion = QuestionVersion.reconstitute(
        id = requireNotNull(id),
        questionId = questionId,
        versionNumber = versionNumber,
        title = title,
        bodyMarkdown = bodyMarkdown,
        environment = environment,
        logs = logs,
        createdBy = createdBy,
        createdAt = createdAt,
    )
}
