package com.quno.qunobackend.application.vote.usecase

import com.quno.qunobackend.domain.vote.Vote
import com.quno.qunobackend.domain.vote.VoteRepository
import com.quno.qunobackend.domain.vote.VoteTargetType

class InMemoryVoteRepository : VoteRepository {
    private val byKey = mutableMapOf<Triple<Long, VoteTargetType, Long>, Vote>()

    override fun save(vote: Vote): Vote {
        byKey[Triple(vote.voterId, vote.targetType, vote.targetId)] = vote
        return vote
    }

    override fun delete(voterId: Long, targetType: VoteTargetType, targetId: Long) {
        byKey.remove(Triple(voterId, targetType, targetId))
    }

    override fun findByVoterAndTarget(voterId: Long, targetType: VoteTargetType, targetId: Long): Vote? =
        byKey[Triple(voterId, targetType, targetId)]

    override fun findByVoter(voterId: Long): List<Vote> =
        byKey.values.filter { it.voterId == voterId }

    override fun sumScore(targetType: VoteTargetType, targetId: Long): Long =
        byKey.values.filter { it.targetType == targetType && it.targetId == targetId }.sumOf { it.value.toLong() }
}
