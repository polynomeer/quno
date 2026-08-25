package com.quno.qunobackend.interfaces.api.metrics

import com.quno.qunobackend.application.metrics.usecase.GetMetricsSnapshotUseCase
import com.quno.qunobackend.domain.metrics.MetricsSnapshot
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/metrics")
class MetricsController(
    private val getMetricsSnapshotUseCase: GetMetricsSnapshotUseCase,
) {

    @GetMapping
    fun get(): MetricsSnapshot = getMetricsSnapshotUseCase.execute()
}
