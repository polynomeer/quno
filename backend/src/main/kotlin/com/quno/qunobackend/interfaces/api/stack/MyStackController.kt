package com.quno.qunobackend.interfaces.api.stack

import com.quno.qunobackend.application.stack.usecase.MyStackUseCase
import com.quno.qunobackend.application.stack.usecase.SuggestStackWardsUseCase
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/** "내 스택" and the Ward suggestions it drives (Phase 63.5, ADR-0078). Always the caller's own. */
@RestController
@RequestMapping("/api/v1/me/stack")
class MyStackController(
    private val myStackUseCase: MyStackUseCase,
    private val suggestStackWardsUseCase: SuggestStackWardsUseCase,
) {
    @GetMapping
    fun get(@AuthenticationPrincipal userId: Long): MyStackResponse = MyStackResponse(myStackUseCase.get(userId).map { it.toResponse() })

    @PutMapping
    fun replace(@AuthenticationPrincipal userId: Long, @Valid @RequestBody request: ReplaceMyStackRequest): MyStackResponse = MyStackResponse(myStackUseCase.replace(userId, request.dependencies.map { it.toDomain() }).map { it.toResponse() })

    @GetMapping("/ward-suggestions")
    fun wardSuggestions(
        @AuthenticationPrincipal userId: Long,
        @RequestParam(defaultValue = "20") limit: Int,
    ): List<StackWardSuggestionResponse> = suggestStackWardsUseCase.execute(userId, limit).map { it.toResponse() }
}
