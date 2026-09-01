package com.quno.qunobackend.interfaces.api.common

import com.quno.qunobackend.domain.answer.AnswerAccessDeniedException
import com.quno.qunobackend.domain.answer.AnswerNotFoundException
import com.quno.qunobackend.domain.answer.AnswerVersionNotFoundException
import com.quno.qunobackend.domain.cluster.AnswerNotAcceptedException
import com.quno.qunobackend.domain.cluster.AnswerNotInClusterException
import com.quno.qunobackend.domain.cluster.CannotClusterWithSelfException
import com.quno.qunobackend.domain.cluster.ClusterNotFoundException
import com.quno.qunobackend.domain.cluster.QuestionNotInAnyClusterException
import com.quno.qunobackend.domain.comment.CommentAccessDeniedException
import com.quno.qunobackend.domain.comment.CommentAlreadyDeletedException
import com.quno.qunobackend.domain.comment.CommentNotFoundException
import com.quno.qunobackend.domain.comment.CommentReplyDepthExceededException
import com.quno.qunobackend.domain.directask.DirectAskAccessDeniedException
import com.quno.qunobackend.domain.directask.DirectAskNotAcceptedException
import com.quno.qunobackend.domain.directask.DirectAskRequestAlreadyRespondedException
import com.quno.qunobackend.domain.directask.DirectAskRequestNotFoundException
import com.quno.qunobackend.domain.directask.DuplicateDirectAskException
import com.quno.qunobackend.domain.directask.SelfDirectAskException
import com.quno.qunobackend.domain.follow.SelfFollowException
import com.quno.qunobackend.domain.livechat.LiveChatRoomNotFoundException
import com.quno.qunobackend.domain.organization.DuplicateOrganizationNameException
import com.quno.qunobackend.domain.organization.EmailDomainVerificationExpiredException
import com.quno.qunobackend.domain.organization.EmailDomainVerificationNotFoundException
import com.quno.qunobackend.domain.organization.InvalidVerificationCodeException
import com.quno.qunobackend.domain.organization.OrganizationNotFoundException
import com.quno.qunobackend.domain.organization.PublicEmailDomainException
import com.quno.qunobackend.domain.organization.VerifiedOrganizationJoinRequiresEmailException
import com.quno.qunobackend.domain.question.QuestionAccessDeniedException
import com.quno.qunobackend.domain.question.QuestionAlreadyResolvedException
import com.quno.qunobackend.domain.question.QuestionNotFoundException
import com.quno.qunobackend.domain.question.QuestionVersionNotFoundException
import com.quno.qunobackend.domain.report.ModeratorAccessDeniedException
import com.quno.qunobackend.domain.report.ReportAlreadyResolvedException
import com.quno.qunobackend.domain.report.ReportNotFoundException
import com.quno.qunobackend.domain.review.QuestionNotRevisedSinceRequestException
import com.quno.qunobackend.domain.review.ReviewRequestAlreadyAddressedException
import com.quno.qunobackend.domain.review.ReviewRequestNotFoundException
import com.quno.qunobackend.domain.review.SelfReviewRequestException
import com.quno.qunobackend.domain.tag.TagNotFoundException
import com.quno.qunobackend.domain.user.DuplicateEmailException
import com.quno.qunobackend.domain.user.DuplicateNicknameException
import com.quno.qunobackend.domain.user.InvalidCredentialsException
import com.quno.qunobackend.domain.user.InvalidTokenException
import com.quno.qunobackend.domain.user.UserNotFoundException
import com.quno.qunobackend.domain.vote.InvalidVoteValueException
import com.quno.qunobackend.domain.vote.SelfVoteException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

data class ErrorResponse(val code: String, val message: String)

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(
        DuplicateEmailException::class,
        DuplicateNicknameException::class,
        QuestionAlreadyResolvedException::class,
        ReviewRequestAlreadyAddressedException::class,
        QuestionNotRevisedSinceRequestException::class,
        AnswerNotInClusterException::class,
        AnswerNotAcceptedException::class,
        ReportAlreadyResolvedException::class,
        CommentAlreadyDeletedException::class,
        DuplicateOrganizationNameException::class,
        DuplicateDirectAskException::class,
        DirectAskRequestAlreadyRespondedException::class,
        DirectAskNotAcceptedException::class,
        EmailDomainVerificationExpiredException::class,
    )
    fun handleConflict(ex: RuntimeException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse("CONFLICT", ex.message.orEmpty()))

    @ExceptionHandler(InvalidCredentialsException::class, InvalidTokenException::class)
    fun handleUnauthorized(ex: RuntimeException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErrorResponse("UNAUTHORIZED", ex.message.orEmpty()))

    @ExceptionHandler(
        UserNotFoundException::class,
        QuestionNotFoundException::class,
        QuestionVersionNotFoundException::class,
        AnswerNotFoundException::class,
        TagNotFoundException::class,
        ReviewRequestNotFoundException::class,
        ClusterNotFoundException::class,
        QuestionNotInAnyClusterException::class,
        CommentNotFoundException::class,
        ReportNotFoundException::class,
        AnswerVersionNotFoundException::class,
        OrganizationNotFoundException::class,
        DirectAskRequestNotFoundException::class,
        EmailDomainVerificationNotFoundException::class,
        LiveChatRoomNotFoundException::class,
    )
    fun handleNotFound(ex: RuntimeException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse("NOT_FOUND", ex.message.orEmpty()))

    @ExceptionHandler(
        QuestionAccessDeniedException::class,
        SelfReviewRequestException::class,
        SelfVoteException::class,
        CommentAccessDeniedException::class,
        SelfFollowException::class,
        ModeratorAccessDeniedException::class,
        AnswerAccessDeniedException::class,
        SelfDirectAskException::class,
        DirectAskAccessDeniedException::class,
        VerifiedOrganizationJoinRequiresEmailException::class,
    )
    fun handleForbidden(ex: RuntimeException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.FORBIDDEN).body(ErrorResponse("FORBIDDEN", ex.message.orEmpty()))

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val message = ex.bindingResult.fieldErrors.joinToString("; ") { "${it.field}: ${it.defaultMessage}" }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse("VALIDATION_ERROR", message))
    }

    @ExceptionHandler(
        CannotClusterWithSelfException::class,
        InvalidVoteValueException::class,
        CommentReplyDepthExceededException::class,
        PublicEmailDomainException::class,
        InvalidVerificationCodeException::class,
    )
    fun handleBadRequest(ex: RuntimeException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse("BAD_REQUEST", ex.message.orEmpty()))
}
