package com.quno.qunobackend.infrastructure.persistence.jpa.repository

import com.quno.qunobackend.infrastructure.persistence.jpa.entity.UserJpaEntity
import org.springframework.data.jpa.repository.JpaRepository

interface UserJpaRepository : JpaRepository<UserJpaEntity, Long> {
    fun findByEmail(email: String): UserJpaEntity?
    fun findByNickname(nickname: String): UserJpaEntity?
    fun existsByEmail(email: String): Boolean
    fun existsByNickname(nickname: String): Boolean
}
