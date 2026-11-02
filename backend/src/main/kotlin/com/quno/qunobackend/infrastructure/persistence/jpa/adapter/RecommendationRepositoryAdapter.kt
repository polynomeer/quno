package com.quno.qunobackend.infrastructure.persistence.jpa.adapter

import com.quno.qunobackend.domain.question.QuestionViewer
import com.quno.qunobackend.domain.recommendation.RecommendationRepository
import com.quno.qunobackend.infrastructure.persistence.jpa.repository.RecommendationJpaRepository
import org.springframework.stereotype.Component

@Component
class RecommendationRepositoryAdapter(
    private val jpaRepository: RecommendationJpaRepository,
) : RecommendationRepository {

    override fun recommendQuestionIdsByTagFollows(userId: Long, limit: Int, viewer: QuestionViewer): List<Long> = jpaRepository.recommendQuestionIdsByTagFollows(userId, limit, viewer.userIdForSql(), viewer.organizationIdsForSql())
}
