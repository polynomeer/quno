package com.quno.qunobackend.interfaces.api.directask

import com.quno.qunobackend.application.directask.dto.ConfirmDirectAskPaymentCommand
import com.quno.qunobackend.application.directask.dto.CreateDirectAskRequestCommand
import com.quno.qunobackend.application.directask.usecase.ConfirmDirectAskPaymentUseCase
import com.quno.qunobackend.application.directask.usecase.CreateDirectAskRequestUseCase
import com.quno.qunobackend.application.directask.usecase.ListMyDirectAsksUseCase
import com.quno.qunobackend.application.directask.usecase.RespondToDirectAskRequestUseCase
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/** Paid via Toss Payments test mode (Phase 25, ADR-0037, superseding Phase 22's free flow) — the
 * target simply posts a normal answer after accepting; the fee itself isn't paid out to them. */
@RestController
@RequestMapping("/api/v1")
class DirectAskController(
    private val createDirectAskRequestUseCase: CreateDirectAskRequestUseCase,
    private val confirmDirectAskPaymentUseCase: ConfirmDirectAskPaymentUseCase,
    private val respondToDirectAskRequestUseCase: RespondToDirectAskRequestUseCase,
    private val listMyDirectAsksUseCase: ListMyDirectAsksUseCase,
) {

    @PostMapping("/questions/{questionId}/direct-asks")
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @AuthenticationPrincipal requesterId: Long,
        @PathVariable questionId: Long,
        @Valid @RequestBody request: CreateDirectAskRequest,
    ): CreateDirectAskRequestResponse {
        val result = createDirectAskRequestUseCase.execute(
            CreateDirectAskRequestCommand(
                questionId = questionId,
                requesterId = requesterId,
                targetUserId = request.targetUserId,
                message = request.message,
            ),
        )
        return result.toResponse()
    }

    @PostMapping("/direct-asks/payments/confirm")
    fun confirmPayment(
        @AuthenticationPrincipal actorId: Long,
        @Valid @RequestBody request: ConfirmDirectAskPaymentRequest,
    ): DirectAskRequestResponse =
        confirmDirectAskPaymentUseCase.execute(
            ConfirmDirectAskPaymentCommand(orderId = request.orderId, paymentKey = request.paymentKey, amount = request.amount, actorId = actorId),
        ).toResponse()

    @PostMapping("/direct-asks/{id}/accept")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun accept(@AuthenticationPrincipal actorId: Long, @PathVariable id: Long) {
        respondToDirectAskRequestUseCase.execute(id, actorId, accept = true)
    }

    @PostMapping("/direct-asks/{id}/decline")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun decline(@AuthenticationPrincipal actorId: Long, @PathVariable id: Long) {
        respondToDirectAskRequestUseCase.execute(id, actorId, accept = false)
    }

    @GetMapping("/me/direct-asks")
    fun listMine(
        @AuthenticationPrincipal userId: Long,
        @RequestParam(defaultValue = "received") role: String,
    ): List<DirectAskRequestListItemResponse> {
        val results = if (role == "sent") listMyDirectAsksUseCase.executeSent(userId) else listMyDirectAsksUseCase.executeReceived(userId)
        return results.map { it.toResponse() }
    }
}
