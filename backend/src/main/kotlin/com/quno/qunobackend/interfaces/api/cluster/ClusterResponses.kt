package com.quno.qunobackend.interfaces.api.cluster

import com.quno.qunobackend.application.cluster.dto.ClusterDetailResult
import com.quno.qunobackend.application.cluster.dto.ClusterResult
import com.quno.qunobackend.interfaces.api.search.QuestionSearchResultResponse
import com.quno.qunobackend.interfaces.api.search.toResponse

data class ClusterResponse(
    val clusterId: Long,
    val memberQuestionIds: List<Long>,
    val representativeAnswerId: Long?,
)

fun ClusterResult.toResponse() = ClusterResponse(
    clusterId = clusterId,
    memberQuestionIds = memberQuestionIds,
    representativeAnswerId = representativeAnswerId,
)

data class ClusterDetailResponse(
    val clusterId: Long,
    val members: List<QuestionSearchResultResponse>,
    val representativeAnswerId: Long?,
)

fun ClusterDetailResult.toResponse() = ClusterDetailResponse(
    clusterId = clusterId,
    members = members.map { it.toResponse() },
    representativeAnswerId = representativeAnswerId,
)
