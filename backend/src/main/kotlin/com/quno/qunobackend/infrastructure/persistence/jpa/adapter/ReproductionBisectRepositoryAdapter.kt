package com.quno.qunobackend.infrastructure.persistence.jpa.adapter

import com.quno.qunobackend.domain.reproduction.BisectDecision
import com.quno.qunobackend.domain.reproduction.BisectStatus
import com.quno.qunobackend.domain.reproduction.ReproductionBisect
import com.quno.qunobackend.domain.reproduction.ReproductionBisectRepository
import com.quno.qunobackend.infrastructure.persistence.jpa.entity.ReproductionBisectJpaEntity
import com.quno.qunobackend.infrastructure.persistence.jpa.repository.ReproductionBisectJpaRepository
import org.springframework.stereotype.Component

@Component
class ReproductionBisectRepositoryAdapter(
    private val jpaRepository: ReproductionBisectJpaRepository,
) : ReproductionBisectRepository {

    override fun save(bisect: ReproductionBisect): ReproductionBisect = jpaRepository.saveAndFlush(bisect.toEntity()).toDomain()

    override fun findById(id: Long): ReproductionBisect? = jpaRepository.findById(id).map { it.toDomain() }.orElse(null)

    override fun findRunning(capsuleId: Long): ReproductionBisect? = jpaRepository.findByCapsuleIdAndStatus(capsuleId, BisectStatus.RUNNING)?.toDomain()

    override fun findLatest(capsuleId: Long): ReproductionBisect? = jpaRepository.findFirstByCapsuleIdOrderByIdDesc(capsuleId)?.toDomain()

    override fun findLatestApproved(capsuleId: Long): ReproductionBisect? = jpaRepository.findFirstByCapsuleIdAndDecisionOrderByIdDesc(capsuleId, BisectDecision.APPROVED)?.toDomain()

    private fun ReproductionBisect.toEntity() = ReproductionBisectJpaEntity(id, capsuleId, fromVersion, toVersion, fromResult, toResult, candidates.joinToString(","), status, lowVersion, highVersion, confidence, runsUsed, decision, createdBy, createdAt, finishedAt)

    private fun ReproductionBisectJpaEntity.toDomain() = ReproductionBisect(id, capsuleId, fromVersion, toVersion, fromResult, toResult, candidates.split(','), status, lowVersion, highVersion, confidence, runsUsed, decision, createdBy, createdAt, finishedAt)
}
