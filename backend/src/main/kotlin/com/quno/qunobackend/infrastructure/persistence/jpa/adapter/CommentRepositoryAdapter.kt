package com.quno.qunobackend.infrastructure.persistence.jpa.adapter

import com.quno.qunobackend.domain.comment.Comment
import com.quno.qunobackend.domain.comment.CommentRepository
import com.quno.qunobackend.domain.comment.CommentTargetType
import com.quno.qunobackend.infrastructure.persistence.jpa.entity.CommentJpaEntity
import com.quno.qunobackend.infrastructure.persistence.jpa.repository.CommentJpaRepository
import org.springframework.stereotype.Component

@Component
class CommentRepositoryAdapter(
    private val jpaRepository: CommentJpaRepository,
) : CommentRepository {

    override fun save(comment: Comment): Comment {
        val entity = CommentJpaEntity(
            id = comment.id,
            targetType = comment.targetType,
            targetId = comment.targetId,
            authorId = comment.authorId,
            parentCommentId = comment.parentCommentId,
            body = comment.body,
            versionNumber = comment.versionNumber,
            deletedAt = comment.deletedAt,
            createdAt = comment.createdAt,
            updatedAt = comment.updatedAt,
        )
        return jpaRepository.save(entity).toDomain()
    }

    override fun findById(id: Long): Comment? = jpaRepository.findById(id).map { it.toDomain() }.orElse(null)

    override fun listByTarget(targetType: CommentTargetType, targetId: Long): List<Comment> =
        jpaRepository.findAllByTargetTypeAndTargetIdOrderByCreatedAtAsc(targetType, targetId).map { it.toDomain() }

    private fun CommentJpaEntity.toDomain(): Comment = Comment.reconstitute(
        id = requireNotNull(id),
        targetType = targetType,
        targetId = targetId,
        authorId = authorId,
        parentCommentId = parentCommentId,
        body = body,
        versionNumber = versionNumber,
        deletedAt = deletedAt,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
