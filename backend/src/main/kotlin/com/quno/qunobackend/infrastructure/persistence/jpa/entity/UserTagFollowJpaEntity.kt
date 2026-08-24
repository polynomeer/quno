package com.quno.qunobackend.infrastructure.persistence.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import java.io.Serializable
import java.time.Instant

data class UserTagFollowId(
    val userId: Long = 0,
    val tagId: Long = 0,
) : Serializable

@Entity
@Table(name = "user_tag_follows")
@IdClass(UserTagFollowId::class)
class UserTagFollowJpaEntity(
    @Id
    @Column(name = "user_id")
    val userId: Long,

    @Id
    @Column(name = "tag_id")
    val tagId: Long,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant,
)
