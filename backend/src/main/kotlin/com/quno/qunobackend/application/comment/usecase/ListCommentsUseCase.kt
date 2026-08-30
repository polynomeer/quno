package com.quno.qunobackend.application.comment.usecase

import com.quno.qunobackend.application.comment.dto.CommentResult
import com.quno.qunobackend.domain.comment.CommentRepository
import com.quno.qunobackend.domain.comment.CommentTargetType
import org.springframework.stereotype.Service

@Service
class ListCommentsUseCase(
    private val commentRepository: CommentRepository,
) {
    /** Includes soft-deleted comments (tombstoned, body nulled by [toResult]) — see ADR-0024 #4. */
    fun execute(targetType: CommentTargetType, targetId: Long): List<CommentResult> =
        commentRepository.listByTarget(targetType, targetId).map { it.toResult() }
}
