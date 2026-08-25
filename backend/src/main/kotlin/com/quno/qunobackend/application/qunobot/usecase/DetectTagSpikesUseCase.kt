package com.quno.qunobackend.application.qunobot.usecase

import com.quno.qunobackend.domain.qunobot.SpikeDetectionRepository
import com.quno.qunobackend.domain.qunobot.TagSpike
import org.springframework.stereotype.Service

/** Read-only reporting model — see ADR-0010, [TagSpike] is reused as-is through to the API layer. */
@Service
class DetectTagSpikesUseCase(
    private val spikeDetectionRepository: SpikeDetectionRepository,
) {
    fun execute(limit: Int): List<TagSpike> = spikeDetectionRepository.findSpikingTags(limit)
}
