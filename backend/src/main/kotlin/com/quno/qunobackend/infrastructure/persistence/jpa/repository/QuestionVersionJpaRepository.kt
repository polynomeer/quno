package com.quno.qunobackend.infrastructure.persistence.jpa.repository

import com.quno.qunobackend.infrastructure.persistence.jpa.entity.QuestionVersionJpaEntity
import org.springframework.data.jpa.repository.JpaRepository

interface QuestionVersionJpaRepository : JpaRepository<QuestionVersionJpaEntity, Long>
