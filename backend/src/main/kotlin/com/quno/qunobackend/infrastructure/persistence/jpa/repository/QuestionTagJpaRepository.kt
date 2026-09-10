package com.quno.qunobackend.infrastructure.persistence.jpa.repository

import com.quno.qunobackend.infrastructure.persistence.jpa.entity.QuestionTagId
import com.quno.qunobackend.infrastructure.persistence.jpa.entity.QuestionTagJpaEntity
import org.springframework.data.jpa.repository.JpaRepository

interface QuestionTagJpaRepository : JpaRepository<QuestionTagJpaEntity, QuestionTagId> {
    fun findAllByQuestionId(questionId: Long): List<QuestionTagJpaEntity>

    fun findAllByQuestionIdIn(questionIds: List<Long>): List<QuestionTagJpaEntity>
}
