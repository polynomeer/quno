package com.quno.qunobackend.infrastructure.persistence.jpa.repository

interface TagSpikeProjection {
    fun getId(): Long
    fun getName(): String
    fun getSlug(): String
    fun getRecentCount(): Long
    fun getBaselineAveragePerDay(): Double
    fun getSpikeRatio(): Double
}
