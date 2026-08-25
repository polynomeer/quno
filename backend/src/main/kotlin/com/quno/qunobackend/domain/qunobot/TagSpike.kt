package com.quno.qunobackend.domain.qunobot

/**
 * A tag whose recent question volume is unusually high compared to its own baseline — see
 * docs/architecture/decisions/0017-manual-outdated-marking-and-spike-detection-scope.md.
 * This flags "something is happening with this tag", not a cause; a human reads the spike.
 */
data class TagSpike(
    val id: Long,
    val name: String,
    val slug: String,
    val recentCount: Long,
    val baselineAveragePerDay: Double,
    val spikeRatio: Double,
)
