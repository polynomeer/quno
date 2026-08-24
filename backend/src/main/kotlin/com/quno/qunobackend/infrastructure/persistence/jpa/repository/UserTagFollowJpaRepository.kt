package com.quno.qunobackend.infrastructure.persistence.jpa.repository

import com.quno.qunobackend.infrastructure.persistence.jpa.entity.UserTagFollowId
import com.quno.qunobackend.infrastructure.persistence.jpa.entity.UserTagFollowJpaEntity
import org.springframework.data.jpa.repository.JpaRepository

interface UserTagFollowJpaRepository : JpaRepository<UserTagFollowJpaEntity, UserTagFollowId> {
    fun findAllByUserId(userId: Long): List<UserTagFollowJpaEntity>
}
