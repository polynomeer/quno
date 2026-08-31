package com.quno.qunobackend.interfaces.api.qunobot

import com.quno.qunobackend.application.qunobot.usecase.GetVersionImpactsUseCase
import com.quno.qunobackend.domain.qunobot.VersionImpact
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/qunobot/version-impacts")
class VersionImpactController(
    private val getVersionImpactsUseCase: GetVersionImpactsUseCase,
) {

    @GetMapping
    fun get(@RequestParam(required = false) limit: Int?): List<VersionImpact> =
        getVersionImpactsUseCase.execute(limit ?: 10)
}
