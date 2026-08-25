package com.quno.qunobackend.application.user.dto

import com.quno.qunobackend.application.answer.dto.AnswerResult
import com.quno.qunobackend.application.search.dto.QuestionSearchResult
import com.quno.qunobackend.application.tag.dto.TagResult
import java.time.Instant

data class SignUpResult(val userId: Long, val email: String, val nickname: String)

data class TokenResult(val accessToken: String, val refreshToken: String)

data class MyProfileResult(val id: Long, val email: String, val nickname: String, val createdAt: Instant)

/** Public-facing — no email, unlike [MyProfileResult]. See docs/product/mvp-scope.md "사용자 프로필 라이트". */
data class UserProfileResult(
    val userId: Long,
    val nickname: String,
    val questions: List<QuestionSearchResult>,
    val answers: List<AnswerResult>,
    val followedTags: List<TagResult>,
)
