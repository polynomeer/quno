package com.quno.qunobackend.infrastructure.persistence.jpa.adapter

import com.quno.qunobackend.domain.question.QuestionPromotionRepository
import com.quno.qunobackend.infrastructure.persistence.jpa.entity.QuestionPromotionJpaEntity
import com.quno.qunobackend.infrastructure.persistence.jpa.repository.QuestionPromotionJpaRepository
import org.springframework.stereotype.Component

@Component
class QuestionPromotionRepositoryAdapter(
    private val jpaRepository: QuestionPromotionJpaRepository,
) : QuestionPromotionRepository {

    override fun findPublicQuestionId(privateQuestionId: Long): Long? = jpaRepository.findById(privateQuestionId).map { it.publicQuestionId }.orElse(null)

    override fun record(privateQuestionId: Long, publicQuestionId: Long, promotedBy: Long) {
        jpaRepository.save(QuestionPromotionJpaEntity(privateQuestionId, publicQuestionId, promotedBy))
    }
}
