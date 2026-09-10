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
import com.quno.qunobackend.domain.directask.DirectAskPaymentNotFoundException
import com.quno.qunobackend.domain.directask.DuplicateDirectAskException
import com.quno.qunobackend.domain.directask.PaymentAlreadyProcessedException
import com.quno.qunobackend.domain.directask.PaymentAmountMismatchException
import com.quno.qunobackend.domain.directask.PaymentConfirmationFailedException
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
import io.sentry.Sentry
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
        PaymentAlreadyProcessedException::class,
        PaymentConfirmationFailedException::class,
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
        DirectAskPaymentNotFoundException::class,
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
        PaymentAmountMismatchException::class,
    )
    fun handleBadRequest(ex: RuntimeException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse("BAD_REQUEST", ex.message.orEmpty()))

    // 위 핸들러들이 다루는 도메인 예외는 예상된 비즈니스 흐름(중복/404/권한 없음 등)이라 에러
    // 트래킹 대상이 아니다 — 여기 걸리는 건 진짜 예기치 못한 버그뿐이라 Sentry로 보낸다. DSN이
    // 없으면(로컬/테스트) Sentry.captureException은 그냥 아무것도 하지 않는다.
    @ExceptionHandler(Exception::class)
    fun handleUnexpected(ex: Exception): ResponseEntity<ErrorResponse> {
        Sentry.captureException(ex)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse("INTERNAL_ERROR", "예기치 못한 오류가 발생했습니다."))
    }
}
