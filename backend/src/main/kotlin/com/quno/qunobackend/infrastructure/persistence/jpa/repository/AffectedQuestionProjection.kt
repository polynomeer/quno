package com.quno.qunobackend.infrastructure.persistence.jpa.repository

interface AffectedQuestionProjection {
    fun getQuestionId(): Long
    fun getQuestionAuthorId(): Long
}
