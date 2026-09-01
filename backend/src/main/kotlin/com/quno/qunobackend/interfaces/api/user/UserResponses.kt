package com.quno.qunobackend.interfaces.api.user

import com.quno.qunobackend.interfaces.api.answer.AnswerResponse
import com.quno.qunobackend.interfaces.api.organization.OrganizationResponse
import com.quno.qunobackend.interfaces.api.search.QuestionSearchResultResponse
import com.quno.qunobackend.interfaces.api.tag.TagResponse
import java.time.Instant

data class MyProfileResponse(
    val id: Long,
    val email: String,
    val nickname: String,
    val acceptsDirectAsk: Boolean,
    val createdAt: Instant,
)

data class UserProfileResponse(
    val userId: Long,
    val nickname: String,
    val questions: List<QuestionSearchResultResponse>,
    val answers: List<AnswerResponse>,
    val followedTags: List<TagResponse>,
    val organizations: List<OrganizationResponse>,
)
