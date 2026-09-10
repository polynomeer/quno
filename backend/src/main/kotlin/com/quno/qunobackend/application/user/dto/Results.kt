package com.quno.qunobackend.application.user.dto

import com.quno.qunobackend.application.answer.dto.AnswerResult
import com.quno.qunobackend.application.organization.dto.OrganizationResult
import com.quno.qunobackend.application.search.dto.QuestionSearchResult
import com.quno.qunobackend.application.tag.dto.TagResult
import java.time.Instant

data class SignUpResult(val userId: Long, val email: String, val nickname: String)

data class TokenResult(val accessToken: String, val refreshToken: String)

data class MyProfileResult(
    val id: Long,
    val email: String,
    val nickname: String,
    val acceptsDirectAsk: Boolean,
    val createdAt: Instant,
)

/** Public-facing — no email, unlike [MyProfileResult]. See docs/product/mvp-scope.md "사용자 프로필 라이트". */
data class UserProfileResult(
    val userId: Long,
    val nickname: String,
    val questions: List<QuestionSearchResult>,
    val answers: List<AnswerResult>,
    val followedTags: List<TagResult>,
    /** Virtual/Community organizations this user has joined (Phase 22, ADR-0034). */
    val organizations: List<OrganizationResult>,
)

/** 개인정보 다운로드 요청(ADR-0046) 응답 — 프로필과 직접 작성한 콘텐츠까지만. 투표/Direct
 * Ask/실시간 채팅 메시지 등은 범위 밖(최소 요건, 필요해지면 확장). */
data class MyDataExportResult(
    val userId: Long,
    val email: String,
    val nickname: String,
    val createdAt: Instant,
    val questions: List<MyDataExportQuestion>,
    val answers: List<MyDataExportAnswer>,
)

data class MyDataExportQuestion(val id: Long, val title: String, val createdAt: Instant)

data class MyDataExportAnswer(val id: Long, val questionId: Long, val createdAt: Instant)
