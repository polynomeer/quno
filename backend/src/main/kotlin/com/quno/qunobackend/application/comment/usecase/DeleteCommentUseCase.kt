package com.quno.qunobackend.application.comment.usecase

import com.quno.qunobackend.domain.comment.CommentAccessDeniedException
import com.quno.qunobackend.domain.comment.CommentNotFoundException
import com.quno.qunobackend.domain.comment.CommentRepository
import org.springframework.stereotype.Service

@Service
class DeleteCommentUseCase(
    private val commentRepository: CommentRepository,
) {
    fun execute(commentId: Long, actorId: Long) {
        val comment = commentRepository.findById(commentId) ?: throw CommentNotFoundException(commentId)
        if (comment.authorId != actorId) throw CommentAccessDeniedException(commentId)
        commentRepository.save(comment.softDelete())
    }
}
