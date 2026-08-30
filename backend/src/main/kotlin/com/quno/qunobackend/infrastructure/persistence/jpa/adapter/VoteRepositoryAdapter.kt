package com.quno.qunobackend.infrastructure.persistence.jpa.adapter

import com.quno.qunobackend.domain.vote.Vote
import com.quno.qunobackend.domain.vote.VoteRepository
import com.quno.qunobackend.domain.vote.VoteTargetType
import com.quno.qunobackend.infrastructure.persistence.jpa.entity.VoteId
import com.quno.qunobackend.infrastructure.persistence.jpa.entity.VoteJpaEntity
import com.quno.qunobackend.infrastructure.persistence.jpa.repository.VoteJpaRepository
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class VoteRepositoryAdapter(
    private val jpaRepository: VoteJpaRepository,
) : VoteRepository {

    override fun save(vote: Vote): Vote {
        val id = VoteId(vote.voterId, vote.targetType, vote.targetId)
        val createdAt = jpaRepository.findById(id).map { it.createdAt }.orElseGet { Instant.now() }
        jpaRepository.save(
            VoteJpaEntity(vote.voterId, vote.targetType, vote.targetId, vote.value, createdAt, Instant.now()),
        )
        return vote
    }

    override fun delete(voterId: Long, targetType: VoteTargetType, targetId: Long) {
        jpaRepository.deleteById(VoteId(voterId, targetType, targetId))
    }

    override fun findByVoterAndTarget(voterId: Long, targetType: VoteTargetType, targetId: Long): Vote? =
        jpaRepository.findById(VoteId(voterId, targetType, targetId)).map { it.toDomain() }.orElse(null)

    override fun findByVoter(voterId: Long): List<Vote> =
        jpaRepository.findAllByVoterId(voterId).map { it.toDomain() }

    override fun sumScore(targetType: VoteTargetType, targetId: Long): Long =
        jpaRepository.sumScore(targetType.name, targetId)

    private fun VoteJpaEntity.toDomain() = Vote(voterId = voterId, targetType = targetType, targetId = targetId, value = value)
}
