package com.quno.qunobackend.domain.qunobot

interface SpikeDetectionRepository {
    /** Ranked by spikeRatio descending. */
    fun findSpikingTags(limit: Int): List<TagSpike>
}
