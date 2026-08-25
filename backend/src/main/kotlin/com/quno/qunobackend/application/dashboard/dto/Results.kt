package com.quno.qunobackend.application.dashboard.dto

import com.quno.qunobackend.application.notification.dto.NotificationResult
import com.quno.qunobackend.application.search.dto.QuestionSearchResult
import com.quno.qunobackend.domain.qunobot.TagSpike

data class TagTrendResult(val id: Long, val name: String, val slug: String, val questionCount: Long)

data class DashboardHeadline(val text: String, val questionId: Long?)

data class DashboardResult(
    val popularQuestions: List<QuestionSearchResult>,
    val wardUpdates: List<NotificationResult>,
    val followingTagsFeed: List<QuestionSearchResult>,
    val trendingTags: List<TagTrendResult>,
    val headline: DashboardHeadline?,
    val resolvedToday: List<QuestionSearchResult>,
    val reopenedKnowledge: List<QuestionSearchResult>,
    val trendingErrors: List<TagSpike>,
)
