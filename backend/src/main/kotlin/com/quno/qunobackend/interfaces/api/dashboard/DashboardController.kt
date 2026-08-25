package com.quno.qunobackend.interfaces.api.dashboard

import com.quno.qunobackend.application.dashboard.usecase.GetDashboardUseCase
import com.quno.qunobackend.interfaces.api.notification.toResponse
import com.quno.qunobackend.interfaces.api.search.toResponse
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/dashboard")
class DashboardController(
    private val getDashboardUseCase: GetDashboardUseCase,
) {

    @GetMapping
    fun get(@AuthenticationPrincipal userId: Long): DashboardResponse {
        val result = getDashboardUseCase.execute(userId)
        return DashboardResponse(
            popularQuestions = result.popularQuestions.map { it.toResponse() },
            wardUpdates = result.wardUpdates.map { it.toResponse() },
            followingTagsFeed = result.followingTagsFeed.map { it.toResponse() },
            trendingTags = result.trendingTags.map {
                TagTrendResponse(id = it.id, name = it.name, slug = it.slug, questionCount = it.questionCount)
            },
        )
    }
}
