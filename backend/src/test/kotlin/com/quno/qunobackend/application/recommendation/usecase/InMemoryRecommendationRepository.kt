package com.quno.qunobackend.application.recommendation.usecase

import com.quno.qunobackend.domain.recommendation.RecommendationRepository

class InMemoryRecommendationRepository : RecommendationRepository {
    var recommendationsByUser: Map<Long, List<Long>> = emptyMap()

    override fun recommendQuestionIdsByTagFollows(userId: Long, limit: Int): List<Long> =
        (recommendationsByUser[userId] ?: emptyList()).take(limit)
}
