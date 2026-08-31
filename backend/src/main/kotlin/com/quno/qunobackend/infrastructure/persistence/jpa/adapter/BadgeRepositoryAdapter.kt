package com.quno.qunobackend.infrastructure.persistence.jpa.adapter

import com.quno.qunobackend.domain.badge.BadgeRepository
import com.quno.qunobackend.infrastructure.persistence.jpa.repository.BadgeJpaRepository
import org.springframework.stereotype.Component

@Component
class BadgeRepositoryAdapter(
    private val jpaRepository: BadgeJpaRepository,
) : BadgeRepository {
    override fun sumVoteScoreReceived(userId: Long): Long = jpaRepository.sumVoteScoreReceived(userId)
}
