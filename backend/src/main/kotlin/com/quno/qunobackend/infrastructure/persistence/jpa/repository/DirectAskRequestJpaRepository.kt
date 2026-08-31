package com.quno.qunobackend.infrastructure.persistence.jpa.repository

import com.quno.qunobackend.domain.directask.DirectAskRequestStatus
import com.quno.qunobackend.infrastructure.persistence.jpa.entity.DirectAskRequestJpaEntity
import org.springframework.data.jpa.repository.JpaRepository

interface DirectAskRequestJpaRepository : JpaRepository<DirectAskRequestJpaEntity, Long> {
    fun existsByQuestionIdAndTargetUserIdAndStatus(questionId: Long, targetUserId: Long, status: DirectAskRequestStatus): Boolean
    fun findAllByRequesterIdOrderByCreatedAtDesc(requesterId: Long): List<DirectAskRequestJpaEntity>
    fun findAllByTargetUserIdOrderByCreatedAtDesc(targetUserId: Long): List<DirectAskRequestJpaEntity>
}
