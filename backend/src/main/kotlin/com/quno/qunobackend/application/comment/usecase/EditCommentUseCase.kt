package com.quno.qunobackend.application.comment.usecase

import com.quno.qunobackend.application.comment.dto.CommentResult
import com.quno.qunobackend.application.comment.dto.EditCommentCommand
import com.quno.qunobackend.domain.comment.CommentAccessDeniedException
import com.quno.qunobackend.domain.comment.CommentAlreadyDeletedException
import com.quno.qunobackend.domain.comment.CommentNotFoundException
import com.quno.qunobackend.domain.comment.CommentRepository
import com.quno.qunobackend.domain.comment.CommentVersion
import com.quno.qunobackend.domain.comment.CommentVersionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** Author-only edit, no notification fan-out (ADR-0031 #2 — a comment edit is treated as a typo
 * fix, not an activity signal worth re-notifying Ward subscribers about). Mentions are not
 * re-parsed on edit. The about-to-be-superseded body is archived under its own (pre-bump) version
 * number *before* the comment is overwritten, so `comments.body` always holds the current text
 * and `comment_versions` holds every past one — mirrors AnswerVersion's "vN is the content once it
 * became current" shape, but Comment skips creating a v1 row at write time (nothing to archive
 * yet) rather than eagerly duplicating storage for the common case of an unedited comment. */
@Service
class EditCommentUseCase(
    private val commentRepository: CommentRepository,
    private val commentVersionRepository: CommentVersionRepository,
) {
    @Transactional
    fun execute(command: EditCommentCommand): CommentResult {
        val comment = commentRepository.findById(command.commentId) ?: throw CommentNotFoundException(command.commentId)
        if (comment.authorId != command.editorId) throw CommentAccessDeniedException(command.commentId)
        if (comment.deletedAt != null) throw CommentAlreadyDeletedException(command.commentId)

        commentVersionRepository.save(
            CommentVersion.create(commentId = requireNotNull(comment.id), versionNumber = comment.versionNumber, body = comment.body),
        )
        val saved = commentRepository.save(comment.edit(command.body))

        return saved.toResult()
    }
}
