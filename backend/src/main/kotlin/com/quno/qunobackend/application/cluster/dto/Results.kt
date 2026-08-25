package com.quno.qunobackend.application.cluster.dto

import com.quno.qunobackend.application.search.dto.QuestionSearchResult

data class ClusterResult(
    val clusterId: Long,
    val memberQuestionIds: List<Long>,
    val representativeAnswerId: Long?,
)

data class ClusterDetailResult(
    val clusterId: Long,
    val members: List<QuestionSearchResult>,
    val representativeAnswerId: Long?,
)
