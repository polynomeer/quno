package com.quno.qunobackend.application.tag.usecase

import com.quno.qunobackend.domain.tag.TagContributor
import com.quno.qunobackend.domain.tag.TagQuestionSort
import com.quno.qunobackend.domain.tag.TagStatsRepository

class InMemoryTagStatsRepository : TagStatsRepository {
    var questionIdsBySort: Map<TagQuestionSort, List<Long>> = emptyMap()
    var contributors: List<TagContributor> = emptyList()
    var relatedTagIds: List<Long> = emptyList()

    /** Records the last `sort` this fake was called with — same rationale as
     * InMemorySearchRepository.lastSort (the real ranking is SQL, verified by curl, not here). */
    var lastSort: TagQuestionSort? = null

    override fun findQuestionIds(tagId: Long, sort: TagQuestionSort, limit: Int): List<Long> {
        lastSort = sort
        return (questionIdsBySort[sort] ?: emptyList()).take(limit)
    }

    override fun findTopContributors(tagId: Long, limit: Int): List<TagContributor> = contributors.take(limit)

    override fun findRelatedTagIds(tagId: Long, limit: Int): List<Long> = relatedTagIds.take(limit)
}
