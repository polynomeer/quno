package com.quno.qunobackend.infrastructure.persistence.jpa.repository

import com.quno.qunobackend.domain.comment.CommentTargetType
import com.quno.qunobackend.infrastructure.persistence.jpa.entity.CommentJpaEntity
import org.springframework.data.jpa.repository.JpaRepository

interface CommentJpaRepository : JpaRepository<CommentJpaEntity, Long> {
    fun findAllByTargetTypeAndTargetIdOrderByCreatedAtAsc(
        targetType: CommentTargetType,
        targetId: Long,
    ): List<CommentJpaEntity>
}
