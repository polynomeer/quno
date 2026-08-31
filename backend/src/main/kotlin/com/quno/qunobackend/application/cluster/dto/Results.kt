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

/** Composes already-existing pieces (Cluster members, Fork lineage, Related Questions) into one
 * response — a data view, not a new computation or storage (Phase 18, ADR-0030). */
data class QuestionGraphResult(
    val questionId: Long,
    val clusterMembers: List<QuestionSearchResult>,
    val forkedFrom: QuestionSearchResult?,
    val forks: List<QuestionSearchResult>,
    val relatedQuestions: List<QuestionSearchResult>,
)
