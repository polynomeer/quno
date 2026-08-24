package com.quno.qunobackend.infrastructure.persistence.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import java.io.Serializable
import java.time.Instant

data class WatchId(
    val userId: Long = 0,
    val questionId: Long = 0,
) : Serializable

@Entity
@Table(name = "watches")
@IdClass(WatchId::class)
class WatchJpaEntity(
    @Id
    @Column(name = "user_id")
    val userId: Long,

    @Id
    @Column(name = "question_id")
    val questionId: Long,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant,
)
