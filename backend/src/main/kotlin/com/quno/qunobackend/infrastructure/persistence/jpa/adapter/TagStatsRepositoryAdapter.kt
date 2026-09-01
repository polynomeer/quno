package com.quno.qunobackend.infrastructure.persistence.jpa.adapter

import com.quno.qunobackend.domain.tag.TagContributor
import com.quno.qunobackend.domain.tag.TagQuestionSort
import com.quno.qunobackend.domain.tag.TagStatsRepository
import com.quno.qunobackend.infrastructure.persistence.jpa.repository.TagStatsJpaRepository
import org.springframework.stereotype.Component

@Component
class TagStatsRepositoryAdapter(
    private val tagStatsJpaRepository: TagStatsJpaRepository,
) : TagStatsRepository {

    override fun findQuestionIds(tagId: Long, sort: TagQuestionSort, limit: Int): List<Long> = when (sort) {
        TagQuestionSort.LATEST -> tagStatsJpaRepository.findLatestQuestionIds(tagId, limit)
        TagQuestionSort.UNANSWERED -> tagStatsJpaRepository.findUnansweredQuestionIds(tagId, limit)
        TagQuestionSort.TOP -> tagStatsJpaRepository.findTopQuestionIds(tagId, limit)
    }

    override fun findTopContributors(tagId: Long, limit: Int): List<TagContributor> =
        tagStatsJpaRepository.findTopContributors(tagId, limit)
            .map { TagContributor(userId = it.getUserId(), nickname = it.getNickname(), answerCount = it.getAnswerCount()) }

    override fun findRelatedTagIds(tagId: Long, limit: Int): List<Long> =
        tagStatsJpaRepository.findRelatedTagIds(tagId, limit)
}
