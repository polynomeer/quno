package com.quno.qunobackend.interfaces.api.directask

import com.quno.qunobackend.application.directask.dto.CreateDirectAskRequestCommand
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

/** No payment (Phase 22, ADR-0034) — the target simply posts a normal answer after accepting. */
@RestController
@RequestMapping("/api/v1")
class DirectAskController(
    private val createDirectAskRequestUseCase: CreateDirectAskRequestUseCase,
    private val respondToDirectAskRequestUseCase: RespondToDirectAskRequestUseCase,
    private val listMyDirectAsksUseCase: ListMyDirectAsksUseCase,
) {

    @PostMapping("/questions/{questionId}/direct-asks")
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @AuthenticationPrincipal requesterId: Long,
        @PathVariable questionId: Long,
        @Valid @RequestBody request: CreateDirectAskRequest,
    ): DirectAskRequestResponse {
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
    fun listMine(@AuthenticationPrincipal userId: Long, @RequestParam(defaultValue = "received") role: String): List<DirectAskRequestResponse> {
        val results = if (role == "sent") listMyDirectAsksUseCase.executeSent(userId) else listMyDirectAsksUseCase.executeReceived(userId)
        return results.map { it.toResponse() }
    }
}
