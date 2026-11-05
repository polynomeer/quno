package com.quno.qunobackend.infrastructure.persistence.jpa.repository

import com.quno.qunobackend.domain.reproduction.BisectDecision
import com.quno.qunobackend.domain.reproduction.BisectStatus
import com.quno.qunobackend.infrastructure.persistence.jpa.entity.ReproductionBisectJpaEntity
import org.springframework.data.jpa.repository.JpaRepository

interface ReproductionBisectJpaRepository : JpaRepository<ReproductionBisectJpaEntity, Long> {
    fun findByCapsuleIdAndStatus(capsuleId: Long, status: BisectStatus): ReproductionBisectJpaEntity?

    fun findFirstByCapsuleIdOrderByIdDesc(capsuleId: Long): ReproductionBisectJpaEntity?

    fun findFirstByCapsuleIdAndDecisionOrderByIdDesc(capsuleId: Long, decision: BisectDecision): ReproductionBisectJpaEntity?
}
