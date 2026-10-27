package com.quno.qunobackend.infrastructure.persistence.jpa.adapter

import com.quno.qunobackend.domain.stack.Ecosystem
import com.quno.qunobackend.domain.stack.StackDependency
import com.quno.qunobackend.domain.stack.StackMatch
import com.quno.qunobackend.domain.stack.StackMatchRepository
import com.quno.qunobackend.domain.stack.UserStackRepository
import com.quno.qunobackend.infrastructure.persistence.jpa.entity.UserStackDependencyJpaEntity
import com.quno.qunobackend.infrastructure.persistence.jpa.repository.UserStackDependencyJpaRepository
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class UserStackRepositoryAdapter(
    private val jpaRepository: UserStackDependencyJpaRepository,
) : UserStackRepository,
    StackMatchRepository {

    override fun replaceForUser(userId: Long, dependencies: List<StackDependency>) {
        jpaRepository.deleteAllByUserId(userId)
        val now = Instant.now()
        jpaRepository.saveAll(
            dependencies.map { UserStackDependencyJpaEntity(userId = userId, ecosystem = it.ecosystem.name, name = it.name, version = it.version, createdAt = now) },
        )
    }

    override fun findByUserId(userId: Long): List<StackDependency> = jpaRepository.findAllByUserIdOrderByIdAsc(userId).map {
        StackDependency(Ecosystem.valueOf(it.ecosystem), it.name, it.version)
    }

    override fun findQuestionsSharingStack(userId: Long, limit: Int): List<StackMatch> = jpaRepository.findQuestionsSharingStack(userId, limit).map {
        StackMatch(questionId = it.getQuestionId(), sharedCount = it.getSharedCount().toInt())
    }
}
