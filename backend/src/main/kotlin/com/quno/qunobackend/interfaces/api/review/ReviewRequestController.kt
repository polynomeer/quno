package com.quno.qunobackend.interfaces.api.review

import com.quno.qunobackend.application.review.dto.CreateReviewRequestCommand
import com.quno.qunobackend.application.review.usecase.CreateReviewRequestUseCase
import com.quno.qunobackend.application.review.usecase.ListReviewRequestsUseCase
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/questions/{questionId}/review-requests")
class ReviewRequestController(
    private val createReviewRequestUseCase: CreateReviewRequestUseCase,
    private val listReviewRequestsUseCase: ListReviewRequestsUseCase,
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @AuthenticationPrincipal requestedBy: Long,
        @PathVariable questionId: Long,
        @Valid @RequestBody request: CreateReviewRequestRequest,
    ): ReviewRequestResponse {
        val result = createReviewRequestUseCase.execute(
            CreateReviewRequestCommand(questionId = questionId, requestedBy = requestedBy, message = request.message),
        )
        return result.toResponse()
    }

    @GetMapping
    fun list(@PathVariable questionId: Long): List<ReviewRequestResponse> =
        listReviewRequestsUseCase.execute(questionId).map { it.toResponse() }
}
