package com.quno.qunobackend.infrastructure.persistence.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "question_view_counts")
class QuestionViewCountJpaEntity(
    @Id
    @Column(name = "question_id")
    val questionId: Long,

    @Column(name = "view_count", nullable = false)
    val viewCount: Long,
)
