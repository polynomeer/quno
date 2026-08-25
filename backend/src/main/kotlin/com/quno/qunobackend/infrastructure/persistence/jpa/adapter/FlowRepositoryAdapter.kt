package com.quno.qunobackend.infrastructure.persistence.jpa.adapter

import com.quno.qunobackend.domain.flow.FlowRepository
import com.quno.qunobackend.infrastructure.persistence.jpa.repository.FlowJpaRepository
import org.springframework.stereotype.Component

@Component
class FlowRepositoryAdapter(
    private val jpaRepository: FlowJpaRepository,
) : FlowRepository {

    override fun findRecentlyReopenedQuestionIds(limit: Int): List<Long> =
        jpaRepository.findRecentlyReopenedQuestionIds(limit)

    override fun findRecentlySuperAnsweredClusterIds(limit: Int): List<Long> =
        jpaRepository.findRecentlySuperAnsweredClusterIds(limit)
}
