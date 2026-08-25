package com.quno.qunobackend.interfaces.api.qunobot

import com.quno.qunobackend.application.qunobot.usecase.DetectTagSpikesUseCase
import com.quno.qunobackend.domain.qunobot.TagSpike
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/qunobot/spikes")
class SpikeDetectionController(
    private val detectTagSpikesUseCase: DetectTagSpikesUseCase,
) {

    @GetMapping
    fun get(@RequestParam(required = false) limit: Int?): List<TagSpike> =
        detectTagSpikesUseCase.execute(limit ?: 10)
}
