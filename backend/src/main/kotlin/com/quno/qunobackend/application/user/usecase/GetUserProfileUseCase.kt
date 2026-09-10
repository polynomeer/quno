package com.quno.qunobackend.application.user.usecase

import com.quno.qunobackend.application.common.AnswerResultAssembler
import com.quno.qunobackend.application.common.QuestionSummaryHydrator
import com.quno.qunobackend.application.organization.usecase.toResult
import com.quno.qunobackend.application.tag.usecase.toResult
import com.quno.qunobackend.application.user.dto.UserProfileResult
import com.quno.qunobackend.domain.answer.AnswerRepository
import com.quno.qunobackend.domain.organization.OrganizationMembershipRepository
import com.quno.qunobackend.domain.organization.OrganizationRepository
import com.quno.qunobackend.domain.question.QuestionRepository
import com.quno.qunobackend.domain.tag.TagRepository
import com.quno.qunobackend.domain.tag.UserTagFollowRepository
import com.quno.qunobackend.domain.user.UserNotFoundException
import com.quno.qunobackend.domain.user.UserRepository
import org.springframework.stereotype.Service

/** Public profile — see docs/product/mvp-scope.md "사용자 프로필 라이트". */
@Service
class GetUserProfileUseCase(
    private val userRepository: UserRepository,
    private val questionRepository: QuestionRepository,
    private val answerRepository: AnswerRepository,
    private val userTagFollowRepository: UserTagFollowRepository,
    private val tagRepository: TagRepository,
    private val organizationRepository: OrganizationRepository,
    private val organizationMembershipRepository: OrganizationMembershipRepository,
    private val hydrator: QuestionSummaryHydrator,
    private val answerResultAssembler: AnswerResultAssembler,
) {
    fun execute(userId: Long): UserProfileResult {
        val user = userRepository.findById(userId) ?: throw UserNotFoundException(userId)

        val questionIds = questionRepository.findAllByAuthorId(userId).mapNotNull { it.id }
        val questions = hydrator.hydrate(questionIds)

        val answers = answerResultAssembler.toResults(answerRepository.findAllByAuthorId(userId))

        // 팔로우 태그/소속 조직 둘 다 id 하나씩 findById(+countMembers)를 부르고 있었다
        // (quality-improvement-plan.md Q-2) — 이 엔드포인트가 공개(ADR-0042)라 배치로 바꾼다.
        val followedTagIds = userTagFollowRepository.findFollowedTagIds(userId)
        val followedTagsById = tagRepository.findAllByIds(followedTagIds).associateBy { it.id }
        val followedTags = followedTagIds.mapNotNull { followedTagsById[it] }.map { it.toResult() }

        val organizationIds = organizationMembershipRepository.findOrganizationIdsByUserId(userId)
        val organizationsById = organizationRepository.findAllByIds(organizationIds).associateBy { it.id }
        val memberCounts = organizationMembershipRepository.countMembersByOrganizations(organizationIds)
        val organizations = organizationIds.mapNotNull { organizationsById[it] }
            .map { it.toResult(memberCount = memberCounts[it.id] ?: 0L) }

        return UserProfileResult(
            userId = userId,
            nickname = user.nickname,
            questions = questions,
            answers = answers,
            followedTags = followedTags,
            organizations = organizations,
        )
    }
}
