package com.quno.qunobackend.application.recommendation.usecase

import com.quno.qunobackend.application.common.QuestionSummaryHydrator
import com.quno.qunobackend.application.search.dto.QuestionSearchResult
import com.quno.qunobackend.domain.question.QuestionViewer
import com.quno.qunobackend.domain.recommendation.RecommendationRepository
import org.springframework.stereotype.Service

@Service
class RecommendQuestionsUseCase(
    private val recommendationRepository: RecommendationRepository,
    private val hydrator: QuestionSummaryHydrator,
) {
    fun execute(viewer: QuestionViewer, limit: Int = 20): List<QuestionSearchResult> = hydrator.hydrate(
        recommendationRepository.recommendQuestionIdsByTagFollows(requireNotNull(viewer.userId), limit, viewer),
        viewer,
    )
}
