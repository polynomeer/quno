package com.quno.qunobackend.application.qunobot.usecase

import com.quno.qunobackend.domain.qunobot.SpikeDetectionRepository
import com.quno.qunobackend.domain.qunobot.TagSpike

class InMemorySpikeDetectionRepository : SpikeDetectionRepository {
    var spikingTags: List<TagSpike> = emptyList()

    override fun findSpikingTags(limit: Int): List<TagSpike> = spikingTags.take(limit)
}
