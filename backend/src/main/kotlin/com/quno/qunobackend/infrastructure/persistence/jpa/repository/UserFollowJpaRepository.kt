package com.quno.qunobackend.infrastructure.persistence.jpa.repository

import com.quno.qunobackend.infrastructure.persistence.jpa.entity.UserFollowId
import com.quno.qunobackend.infrastructure.persistence.jpa.entity.UserFollowJpaEntity
import org.springframework.data.jpa.repository.JpaRepository

interface UserFollowJpaRepository : JpaRepository<UserFollowJpaEntity, UserFollowId> {
    fun findAllByFollowerId(followerId: Long): List<UserFollowJpaEntity>
}
