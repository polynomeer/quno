package com.quno.qunobackend.infrastructure.persistence.jpa.adapter

import com.quno.qunobackend.domain.stack.Ecosystem
import com.quno.qunobackend.domain.stack.QuestionDependencyRepository
import com.quno.qunobackend.domain.stack.StackDependency
import com.quno.qunobackend.infrastructure.persistence.jpa.entity.QuestionDependencyJpaEntity
import com.quno.qunobackend.infrastructure.persistence.jpa.repository.QuestionDependencyJpaRepository
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class QuestionDependencyRepositoryAdapter(
    private val jpaRepository: QuestionDependencyJpaRepository,
) : QuestionDependencyRepository {

    override fun replaceForQuestion(questionId: Long, versionNumber: Int, dependencies: List<StackDependency>) {
        jpaRepository.deleteAllByQuestionId(questionId)
        val now = Instant.now()
        jpaRepository.saveAll(
            dependencies.map {
                QuestionDependencyJpaEntity(
                    questionId = questionId,
                    versionNumber = versionNumber,
                    ecosystem = it.ecosystem.name,
                    name = it.name,
                    version = it.version,
                    createdAt = now,
                )
            },
        )
    }

    override fun findByQuestionId(questionId: Long): List<StackDependency> = jpaRepository.findAllByQuestionIdOrderByIdAsc(questionId).map {
        StackDependency(Ecosystem.valueOf(it.ecosystem), it.name, it.version)
    }

    override fun findByQuestionIds(questionIds: Collection<Long>): Map<Long, List<StackDependency>> {
        if (questionIds.isEmpty()) return emptyMap()
        return jpaRepository.findAllByQuestionIdInOrderByIdAsc(questionIds).groupBy({ it.questionId }, { StackDependency(Ecosystem.valueOf(it.ecosystem), it.name, it.version) })
    }
}
