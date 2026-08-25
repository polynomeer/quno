package com.quno.qunobackend.infrastructure.persistence.jpa.repository

interface TagTrendProjection {
    fun getId(): Long
    fun getName(): String
    fun getSlug(): String
    fun getQuestionCount(): Long
}
