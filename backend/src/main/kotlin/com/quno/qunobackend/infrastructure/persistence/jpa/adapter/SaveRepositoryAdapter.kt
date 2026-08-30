package com.quno.qunobackend.infrastructure.persistence.jpa.adapter

import com.quno.qunobackend.domain.save.SaveRepository
import com.quno.qunobackend.infrastructure.persistence.jpa.entity.SaveId
import com.quno.qunobackend.infrastructure.persistence.jpa.entity.SaveJpaEntity
import com.quno.qunobackend.infrastructure.persistence.jpa.repository.SaveJpaRepository
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class SaveRepositoryAdapter(
    private val jpaRepository: SaveJpaRepository,
) : SaveRepository {

    override fun save(userId: Long, questionId: Long) {
        val id = SaveId(userId, questionId)
        if (!jpaRepository.existsById(id)) {
            jpaRepository.save(SaveJpaEntity(userId, questionId, Instant.now()))
        }
    }

    override fun unsave(userId: Long, questionId: Long) {
        jpaRepository.deleteById(SaveId(userId, questionId))
    }

    override fun isSaved(userId: Long, questionId: Long): Boolean =
        jpaRepository.existsById(SaveId(userId, questionId))

    override fun findSavedQuestionIds(userId: Long): List<Long> =
        jpaRepository.findAllByUserId(userId).map { it.questionId }
}
