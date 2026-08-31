package com.quno.qunobackend.application.badge.usecase

import com.quno.qunobackend.domain.badge.BadgeRepository
import com.quno.qunobackend.domain.badge.BadgeStats
import com.quno.qunobackend.domain.badge.BadgeType
import com.quno.qunobackend.domain.reputation.ReputationRepository
import com.quno.qunobackend.domain.user.UserNotFoundException
import com.quno.qunobackend.domain.user.UserRepository
import org.springframework.stereotype.Service

/** Read-only reporting model (ADR-0027, same treatment as [GetUserReputationUseCase] — no
 * persisted "earned at" timestamp, recomputed from scratch on every call). */
@Service
class GetUserBadgesUseCase(
    private val userRepository: UserRepository,
    private val reputationRepository: ReputationRepository,
    private val badgeRepository: BadgeRepository,
) {
    fun execute(userId: Long): List<BadgeType> {
        userRepository.findById(userId) ?: throw UserNotFoundException(userId)
        val reputation = reputationRepository.compute(userId)
        val stats = BadgeStats(
            questionCount = reputation.questionCount,
            answerCount = reputation.answerCount,
            acceptedAnswerCount = reputation.acceptedAnswerCount,
            superAnswerCount = reputation.superAnswerCount,
            voteScoreReceived = badgeRepository.sumVoteScoreReceived(userId),
        )
        return BadgeType.earnedBy(stats)
    }
}
