package com.quno.qunobackend.infrastructure.persistence.jpa.repository

import java.time.LocalDate

interface VersionImpactProjection {
    fun getQuestionId(): Long
    fun getQuestionTitle(): String
    fun getTagSlug(): String
    fun getProductSlug(): String
    fun getLatestVersion(): String
    fun getLatestReleaseDate(): LocalDate
}
