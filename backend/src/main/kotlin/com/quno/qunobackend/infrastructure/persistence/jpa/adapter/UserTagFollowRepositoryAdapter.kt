package com.quno.qunobackend.infrastructure.persistence.jpa.adapter

import com.quno.qunobackend.domain.tag.UserTagFollowRepository
import com.quno.qunobackend.infrastructure.persistence.jpa.entity.UserTagFollowId
import com.quno.qunobackend.infrastructure.persistence.jpa.entity.UserTagFollowJpaEntity
import com.quno.qunobackend.infrastructure.persistence.jpa.repository.UserTagFollowJpaRepository
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class UserTagFollowRepositoryAdapter(
    private val jpaRepository: UserTagFollowJpaRepository,
) : UserTagFollowRepository {

    override fun follow(userId: Long, tagId: Long) {
        val id = UserTagFollowId(userId, tagId)
        if (!jpaRepository.existsById(id)) {
            jpaRepository.save(UserTagFollowJpaEntity(userId, tagId, Instant.now()))
        }
    }

    override fun unfollow(userId: Long, tagId: Long) {
        jpaRepository.deleteById(UserTagFollowId(userId, tagId))
    }

    override fun isFollowing(userId: Long, tagId: Long): Boolean = jpaRepository.existsById(UserTagFollowId(userId, tagId))

    override fun findFollowedTagIds(userId: Long): List<Long> = jpaRepository.findAllByUserId(userId).map { it.tagId }
}
