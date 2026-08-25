package com.quno.qunobackend.infrastructure.persistence.jpa.repository

import com.quno.qunobackend.infrastructure.persistence.jpa.entity.QuestionClusterJpaEntity
import org.springframework.data.jpa.repository.JpaRepository

interface QuestionClusterJpaRepository : JpaRepository<QuestionClusterJpaEntity, Long>
