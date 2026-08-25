package com.quno.qunobackend.interfaces.api.flow

import com.quno.qunobackend.application.flow.usecase.GetActivityFeedUseCase
import com.quno.qunobackend.domain.flow.FlowCard
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/flow")
class FlowController(
    private val getActivityFeedUseCase: GetActivityFeedUseCase,
) {

    @GetMapping
    fun get(@RequestParam(required = false) limit: Int?): List<FlowCard> =
        getActivityFeedUseCase.execute(limit ?: 5)
}
