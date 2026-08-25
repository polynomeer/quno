package com.quno.qunobackend.infrastructure.persistence.jpa.repository

interface UserReputationProjection {
    fun getQuestionCount(): Long
    fun getAnswerCount(): Long
    fun getAcceptedAnswerCount(): Long
    fun getSuperAnswerCount(): Long
}
