package com.quno.qunobackend.infrastructure.persistence.jpa.adapter

import com.quno.qunobackend.domain.reputation.ReputationRepository
import com.quno.qunobackend.domain.reputation.UserReputation
import com.quno.qunobackend.infrastructure.persistence.jpa.repository.ReputationJpaRepository
import org.springframework.stereotype.Component

@Component
class ReputationRepositoryAdapter(
    private val jpaRepository: ReputationJpaRepository,
) : ReputationRepository {

    override fun compute(userId: Long): UserReputation {
        val row = jpaRepository.computeFor(userId)
        return UserReputation(
            userId = userId,
            questionCount = row.getQuestionCount(),
            answerCount = row.getAnswerCount(),
            acceptedAnswerCount = row.getAcceptedAnswerCount(),
            superAnswerCount = row.getSuperAnswerCount(),
        )
    }
}
