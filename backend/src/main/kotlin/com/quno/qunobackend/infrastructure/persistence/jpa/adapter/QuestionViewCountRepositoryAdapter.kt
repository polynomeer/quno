package com.quno.qunobackend.infrastructure.persistence.jpa.adapter

import com.quno.qunobackend.domain.question.QuestionViewCountRepository
import com.quno.qunobackend.infrastructure.persistence.jpa.repository.QuestionViewCountJpaRepository
import org.springframework.stereotype.Component

@Component
class QuestionViewCountRepositoryAdapter(
    private val jpaRepository: QuestionViewCountJpaRepository,
) : QuestionViewCountRepository {

    override fun increment(questionId: Long) = jpaRepository.increment(questionId)

    override fun countByQuestionId(questionId: Long): Long = jpaRepository.findById(questionId).map { it.viewCount }.orElse(0L)

    override fun countsByQuestionIds(questionIds: List<Long>): Map<Long, Long> {
        if (questionIds.isEmpty()) return emptyMap()
        return jpaRepository.findAllByQuestionIdIn(questionIds).associate { it.questionId to it.viewCount }
    }
}
