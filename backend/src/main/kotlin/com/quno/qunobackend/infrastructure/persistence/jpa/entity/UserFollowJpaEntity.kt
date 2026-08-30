package com.quno.qunobackend.infrastructure.persistence.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import java.io.Serializable
import java.time.Instant

data class UserFollowId(
    val followerId: Long = 0,
    val followeeId: Long = 0,
) : Serializable

@Entity
@Table(name = "user_follows")
@IdClass(UserFollowId::class)
class UserFollowJpaEntity(
    @Id
    @Column(name = "follower_id")
    val followerId: Long,

    @Id
    @Column(name = "followee_id")
    val followeeId: Long,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant,
)
