package com.quno.qunobackend.interfaces.api.dashboard

import com.quno.qunobackend.domain.qunobot.TagSpike
import com.quno.qunobackend.interfaces.api.notification.NotificationResponse
import com.quno.qunobackend.interfaces.api.search.QuestionSearchResultResponse

data class TagTrendResponse(val id: Long, val name: String, val slug: String, val questionCount: Long)

data class DashboardHeadlineResponse(val text: String, val questionId: Long?)

data class DashboardResponse(
    val popularQuestions: List<QuestionSearchResultResponse>,
    val wardUpdates: List<NotificationResponse>,
    val followingTagsFeed: List<QuestionSearchResultResponse>,
    val trendingTags: List<TagTrendResponse>,
    val headline: DashboardHeadlineResponse?,
    val resolvedToday: List<QuestionSearchResultResponse>,
    val reopenedKnowledge: List<QuestionSearchResultResponse>,
    val trendingErrors: List<TagSpike>,
)
