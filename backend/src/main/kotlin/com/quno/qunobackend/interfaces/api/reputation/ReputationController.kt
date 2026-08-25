package com.quno.qunobackend.interfaces.api.reputation

import com.quno.qunobackend.application.reputation.usecase.GetUserReputationUseCase
import com.quno.qunobackend.domain.reputation.UserReputation
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/users/{id}/reputation")
class ReputationController(
    private val getUserReputationUseCase: GetUserReputationUseCase,
) {

    @GetMapping
    fun get(@PathVariable id: Long): UserReputation = getUserReputationUseCase.execute(id)
}
