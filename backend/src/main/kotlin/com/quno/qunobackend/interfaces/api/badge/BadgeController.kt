package com.quno.qunobackend.interfaces.api.badge

import com.quno.qunobackend.application.badge.usecase.GetUserBadgesUseCase
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/users/{id}/badges")
class BadgeController(
    private val getUserBadgesUseCase: GetUserBadgesUseCase,
) {

    @GetMapping
    fun get(@PathVariable id: Long): List<BadgeResponse> =
        getUserBadgesUseCase.execute(id).map { BadgeResponse(type = it, tier = it.tier) }
}
