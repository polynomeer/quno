package com.quno.qunobackend.interfaces.api.comment

import com.quno.qunobackend.application.comment.dto.CreateCommentCommand
import com.quno.qunobackend.application.comment.dto.EditCommentCommand
import com.quno.qunobackend.application.comment.usecase.CreateCommentUseCase
import com.quno.qunobackend.application.comment.usecase.DeleteCommentUseCase
import com.quno.qunobackend.application.comment.usecase.EditCommentUseCase
import com.quno.qunobackend.application.comment.usecase.ListCommentVersionsUseCase
import com.quno.qunobackend.application.comment.usecase.ListCommentsUseCase
import com.quno.qunobackend.domain.comment.CommentTargetType
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/** Clarification comments on questions and answers — up to one level of reply nesting, editable
 * with append-only history, @mention notifications on creation (see ADR-0024, ADR-0031). Not QPR
 * ReviewRequest, which models a different "request more info → revise → re-request" workflow. */
@RestController
@RequestMapping("/api/v1")
class CommentController(
    private val createCommentUseCase: CreateCommentUseCase,
    private val listCommentsUseCase: ListCommentsUseCase,
    private val deleteCommentUseCase: DeleteCommentUseCase,
    private val editCommentUseCase: EditCommentUseCase,
    private val listCommentVersionsUseCase: ListCommentVersionsUseCase,
) {

    @PostMapping("/questions/{questionId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    fun commentOnQuestion(
        @AuthenticationPrincipal authorId: Long,
        @PathVariable questionId: Long,
        @Valid @RequestBody request: CreateCommentRequest,
    ): CommentResponse = createCommentUseCase.execute(
        CreateCommentCommand(CommentTargetType.QUESTION, questionId, authorId, request.body, request.parentCommentId),
    ).toResponse()

    @GetMapping("/questions/{questionId}/comments")
    fun listQuestionComments(@PathVariable questionId: Long): List<CommentResponse> =
        listCommentsUseCase.execute(CommentTargetType.QUESTION, questionId).map { it.toResponse() }

    @PostMapping("/answers/{answerId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    fun commentOnAnswer(
        @AuthenticationPrincipal authorId: Long,
        @PathVariable answerId: Long,
        @Valid @RequestBody request: CreateCommentRequest,
    ): CommentResponse = createCommentUseCase.execute(
        CreateCommentCommand(CommentTargetType.ANSWER, answerId, authorId, request.body, request.parentCommentId),
    ).toResponse()

    @GetMapping("/answers/{answerId}/comments")
    fun listAnswerComments(@PathVariable answerId: Long): List<CommentResponse> =
        listCommentsUseCase.execute(CommentTargetType.ANSWER, answerId).map { it.toResponse() }

    @PutMapping("/comments/{commentId}")
    fun edit(
        @AuthenticationPrincipal editorId: Long,
        @PathVariable commentId: Long,
        @Valid @RequestBody request: EditCommentRequest,
    ): CommentResponse = editCommentUseCase.execute(EditCommentCommand(commentId, editorId, request.body)).toResponse()

    @GetMapping("/comments/{commentId}/versions")
    fun versions(@PathVariable commentId: Long): List<CommentVersionResponse> =
        listCommentVersionsUseCase.execute(commentId).map { it.toResponse() }

    @DeleteMapping("/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@AuthenticationPrincipal actorId: Long, @PathVariable commentId: Long) {
        deleteCommentUseCase.execute(commentId, actorId)
    }
}
