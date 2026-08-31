package com.quno.qunobackend.infrastructure.persistence.jpa.entity

import com.quno.qunobackend.domain.user.Role
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "users")
class UserJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, unique = true)
    val email: String,

    @Column(nullable = false, unique = true)
    val nickname: String,

    @Column(name = "password_hash", nullable = false)
    val passwordHash: String,

    @Column(name = "is_active", nullable = false)
    val isActive: Boolean,

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    val role: Role,

    @Column(name = "accepts_direct_ask", nullable = false)
    val acceptsDirectAsk: Boolean,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant,

    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant,
)
