package com.quno.qunobackend.infrastructure.persistence.jpa.repository

import com.quno.qunobackend.infrastructure.persistence.jpa.entity.VoteId
import com.quno.qunobackend.infrastructure.persistence.jpa.entity.VoteJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface VoteJpaRepository : JpaRepository<VoteJpaEntity, VoteId> {
    fun findAllByVoterId(voterId: Long): List<VoteJpaEntity>

    @Query(
        value = """
            SELECT COALESCE(SUM(value), 0) FROM votes
            WHERE target_type = :targetType AND target_id = :targetId
        """,
        nativeQuery = true,
    )
    fun sumScore(@Param("targetType") targetType: String, @Param("targetId") targetId: Long): Long
}
