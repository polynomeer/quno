package com.quno.qunobackend.application.user.usecase

import com.quno.qunobackend.application.common.AnswerResultAssembler
import com.quno.qunobackend.application.common.QuestionSummaryHydrator
import com.quno.qunobackend.application.organization.usecase.toResult
import com.quno.qunobackend.application.tag.dto.TagResult
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

        val followedTags = userTagFollowRepository.findFollowedTagIds(userId)
            .mapNotNull { tagRepository.findById(it) }
            .map { TagResult(id = requireNotNull(it.id), name = it.name, slug = it.slug) }

        val organizations = organizationMembershipRepository.findOrganizationIdsByUserId(userId)
            .mapNotNull { organizationRepository.findById(it) }
            .map { it.toResult(memberCount = organizationMembershipRepository.countMembers(requireNotNull(it.id))) }

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
