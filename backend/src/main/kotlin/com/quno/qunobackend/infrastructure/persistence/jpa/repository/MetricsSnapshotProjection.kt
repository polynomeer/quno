package com.quno.qunobackend.infrastructure.persistence.jpa.repository

interface MetricsSnapshotProjection {
    fun getTotalQuestions(): Long
    fun getRevisedQuestions(): Long
    fun getAnsweredQuestions(): Long
    fun getResolvedQuestions(): Long
    fun getWatchedQuestions(): Long
    fun getLivingQuestions(): Long
}
