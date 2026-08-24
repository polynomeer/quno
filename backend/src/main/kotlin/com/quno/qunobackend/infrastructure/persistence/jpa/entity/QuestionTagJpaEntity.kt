package com.quno.qunobackend.infrastructure.persistence.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import java.io.Serializable

data class QuestionTagId(
    val questionId: Long = 0,
    val tagId: Long = 0,
) : Serializable

@Entity
@Table(name = "question_tags")
@IdClass(QuestionTagId::class)
class QuestionTagJpaEntity(
    @Id
    @Column(name = "question_id")
    val questionId: Long,

    @Id
    @Column(name = "tag_id")
    val tagId: Long,
)
