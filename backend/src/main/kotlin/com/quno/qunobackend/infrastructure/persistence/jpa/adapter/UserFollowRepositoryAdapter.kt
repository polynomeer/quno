package com.quno.qunobackend.infrastructure.persistence.jpa.adapter

import com.quno.qunobackend.domain.follow.UserFollowRepository
import com.quno.qunobackend.infrastructure.persistence.jpa.entity.UserFollowId
import com.quno.qunobackend.infrastructure.persistence.jpa.entity.UserFollowJpaEntity
import com.quno.qunobackend.infrastructure.persistence.jpa.repository.UserFollowJpaRepository
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class UserFollowRepositoryAdapter(
    private val jpaRepository: UserFollowJpaRepository,
) : UserFollowRepository {

    override fun follow(followerId: Long, followeeId: Long) {
        val id = UserFollowId(followerId, followeeId)
        if (!jpaRepository.existsById(id)) {
            jpaRepository.save(UserFollowJpaEntity(followerId, followeeId, Instant.now()))
        }
    }

    override fun unfollow(followerId: Long, followeeId: Long) {
        jpaRepository.deleteById(UserFollowId(followerId, followeeId))
    }

    override fun isFollowing(followerId: Long, followeeId: Long): Boolean =
        jpaRepository.existsById(UserFollowId(followerId, followeeId))

    override fun findFolloweeIds(followerId: Long): List<Long> =
        jpaRepository.findAllByFollowerId(followerId).map { it.followeeId }
}
