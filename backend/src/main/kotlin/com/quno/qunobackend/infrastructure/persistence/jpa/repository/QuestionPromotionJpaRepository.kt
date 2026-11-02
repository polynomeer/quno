package com.quno.qunobackend.infrastructure.persistence.jpa.repository

import com.quno.qunobackend.infrastructure.persistence.jpa.entity.QuestionPromotionJpaEntity
import org.springframework.data.jpa.repository.JpaRepository

interface QuestionPromotionJpaRepository : JpaRepository<QuestionPromotionJpaEntity, Long>
