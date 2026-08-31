package com.quno.qunobackend.infrastructure.persistence.jpa.adapter

import com.quno.qunobackend.domain.badge.BadgeRepository
import com.quno.qunobackend.domain.reputation.ReputationRepository
import com.quno.qunobackend.domain.reputation.UserReputation
import com.quno.qunobackend.infrastructure.persistence.jpa.repository.ReputationJpaRepository
import org.springframework.stereotype.Component

/** Reuses [BadgeRepository.sumVoteScoreReceived] for the vote term (Phase 20, ADR-0032) rather
 * than duplicating the query — Badge and Reputation are already the same Bounded Context
 * (activity-based trust signals, see docs/architecture/domain-model.md). */
@Component
class ReputationRepositoryAdapter(
    private val jpaRepository: ReputationJpaRepository,
    private val badgeRepository: BadgeRepository,
) : ReputationRepository {

    override fun compute(userId: Long): UserReputation {
        val row = jpaRepository.computeFor(userId)
        return UserReputation(
            userId = userId,
            questionCount = row.getQuestionCount(),
            answerCount = row.getAnswerCount(),
            acceptedAnswerCount = row.getAcceptedAnswerCount(),
            superAnswerCount = row.getSuperAnswerCount(),
            voteScoreReceived = badgeRepository.sumVoteScoreReceived(userId),
        )
    }
}
