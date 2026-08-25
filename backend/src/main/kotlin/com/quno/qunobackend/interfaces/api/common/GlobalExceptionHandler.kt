package com.quno.qunobackend.interfaces.api.common

import com.quno.qunobackend.domain.answer.AnswerNotFoundException
import com.quno.qunobackend.domain.cluster.AnswerNotAcceptedException
import com.quno.qunobackend.domain.cluster.AnswerNotInClusterException
import com.quno.qunobackend.domain.cluster.CannotClusterWithSelfException
import com.quno.qunobackend.domain.cluster.ClusterNotFoundException
import com.quno.qunobackend.domain.cluster.ClustersAlreadyDistinctException
import com.quno.qunobackend.domain.cluster.QuestionNotInAnyClusterException
import com.quno.qunobackend.domain.question.QuestionAccessDeniedException
import com.quno.qunobackend.domain.question.QuestionAlreadyResolvedException
import com.quno.qunobackend.domain.question.QuestionNotFoundException
import com.quno.qunobackend.domain.question.QuestionVersionNotFoundException
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
        ClustersAlreadyDistinctException::class,
        AnswerNotInClusterException::class,
        AnswerNotAcceptedException::class,
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
    )
    fun handleNotFound(ex: RuntimeException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse("NOT_FOUND", ex.message.orEmpty()))

    @ExceptionHandler(QuestionAccessDeniedException::class, SelfReviewRequestException::class)
    fun handleForbidden(ex: RuntimeException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.FORBIDDEN).body(ErrorResponse("FORBIDDEN", ex.message.orEmpty()))

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val message = ex.bindingResult.fieldErrors.joinToString("; ") { "${it.field}: ${it.defaultMessage}" }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse("VALIDATION_ERROR", message))
    }

    @ExceptionHandler(CannotClusterWithSelfException::class)
    fun handleBadRequest(ex: RuntimeException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse("BAD_REQUEST", ex.message.orEmpty()))
}
