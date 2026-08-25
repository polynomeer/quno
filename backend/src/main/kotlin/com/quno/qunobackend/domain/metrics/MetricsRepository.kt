package com.quno.qunobackend.domain.metrics

interface MetricsRepository {
    fun snapshot(): MetricsSnapshot
}
