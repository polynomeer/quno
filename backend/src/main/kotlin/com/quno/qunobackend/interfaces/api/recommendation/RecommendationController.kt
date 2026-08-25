package com.quno.qunobackend.interfaces.api.recommendation

import com.quno.qunobackend.application.recommendation.usecase.RecommendQuestionsUseCase
import com.quno.qunobackend.interfaces.api.search.QuestionSearchResultResponse
import com.quno.qunobackend.interfaces.api.search.toResponse
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/recommendations")
class RecommendationController(
    private val recommendQuestionsUseCase: RecommendQuestionsUseCase,
) {

    /** [source] is accepted for forward compatibility; tag-follow scoring is the only MVP strategy. */
    @GetMapping("/questions")
    fun recommendQuestions(
        @AuthenticationPrincipal userId: Long,
        @RequestParam(required = false, defaultValue = "tags") source: String,
        @RequestParam(required = false) limit: Int?,
    ): List<QuestionSearchResultResponse> =
        recommendQuestionsUseCase.execute(userId, limit ?: 20).map { it.toResponse() }
}
