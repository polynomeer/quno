package com.quno.qunobackend.application.flow.usecase

import com.quno.qunobackend.domain.flow.FlowRepository

class InMemoryFlowRepository : FlowRepository {
    var reopenedQuestionIds: List<Long> = emptyList()
    var superAnsweredClusterIds: List<Long> = emptyList()

    override fun findRecentlyReopenedQuestionIds(limit: Int): List<Long> = reopenedQuestionIds.take(limit)

    override fun findRecentlySuperAnsweredClusterIds(limit: Int): List<Long> = superAnsweredClusterIds.take(limit)
}
