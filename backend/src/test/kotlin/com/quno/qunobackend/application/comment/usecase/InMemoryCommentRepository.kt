package com.quno.qunobackend.application.comment.usecase

import com.quno.qunobackend.domain.comment.Comment
import com.quno.qunobackend.domain.comment.CommentRepository
import com.quno.qunobackend.domain.comment.CommentTargetType

class InMemoryCommentRepository : CommentRepository {
    private val byId = mutableMapOf<Long, Comment>()
    private var nextId = 1L

    override fun save(comment: Comment): Comment {
        val saved = if (comment.id == null) {
            Comment.reconstitute(
                id = nextId++,
                targetType = comment.targetType,
                targetId = comment.targetId,
                authorId = comment.authorId,
                body = comment.body,
                deletedAt = comment.deletedAt,
                createdAt = comment.createdAt,
                updatedAt = comment.updatedAt,
            )
        } else {
            comment
        }
        byId[requireNotNull(saved.id)] = saved
        return saved
    }

    override fun findById(id: Long): Comment? = byId[id]

    override fun listByTarget(targetType: CommentTargetType, targetId: Long): List<Comment> =
        byId.values.filter { it.targetType == targetType && it.targetId == targetId }.sortedBy { it.createdAt }
}
