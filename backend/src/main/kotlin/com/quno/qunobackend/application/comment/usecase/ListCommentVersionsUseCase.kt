package com.quno.qunobackend.application.comment.usecase

import com.quno.qunobackend.application.comment.dto.CommentVersionResult
import com.quno.qunobackend.domain.comment.CommentNotFoundException
import com.quno.qunobackend.domain.comment.CommentRepository
import com.quno.qunobackend.domain.comment.CommentVersionRepository
import org.springframework.stereotype.Service

@Service
class ListCommentVersionsUseCase(
    private val commentRepository: CommentRepository,
    private val commentVersionRepository: CommentVersionRepository,
) {
    fun execute(commentId: Long): List<CommentVersionResult> {
        commentRepository.findById(commentId) ?: throw CommentNotFoundException(commentId)
        return commentVersionRepository.findAllByCommentIdOrderByVersionNumberAsc(commentId)
            .map { CommentVersionResult(versionNumber = it.versionNumber, body = it.body, createdAt = it.createdAt) }
    }
}
