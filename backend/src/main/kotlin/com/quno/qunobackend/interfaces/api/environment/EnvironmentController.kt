package com.quno.qunobackend.interfaces.api.environment

import com.quno.qunobackend.application.snapshot.ListEnvironmentGroupsUseCase
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

/** Distinct environments across public questions (Phase 93.6, ADR-0089). */
@RestController
@RequestMapping("/api/v1/environments")
class EnvironmentController(
    private val listUseCase: ListEnvironmentGroupsUseCase,
) {
    @GetMapping
    fun list(@RequestParam(required = false) limit: Int?): List<EnvironmentGroupResponse> = listUseCase.execute(limit ?: 20).map {
        EnvironmentGroupResponse(it.fingerprint, it.core.label, it.questionCount, it.lastSeenAt)
    }
}

data class EnvironmentGroupResponse(val fingerprint: String, val label: String, val questionCount: Long, val lastSeenAt: Instant)
