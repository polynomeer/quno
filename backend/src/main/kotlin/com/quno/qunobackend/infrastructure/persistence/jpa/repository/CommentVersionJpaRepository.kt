package com.quno.qunobackend.infrastructure.persistence.jpa.repository

import com.quno.qunobackend.infrastructure.persistence.jpa.entity.CommentVersionJpaEntity
import org.springframework.data.jpa.repository.JpaRepository

interface CommentVersionJpaRepository : JpaRepository<CommentVersionJpaEntity, Long> {
    fun findAllByCommentIdOrderByVersionNumberAsc(commentId: Long): List<CommentVersionJpaEntity>
}
