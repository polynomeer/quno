package com.quno.qunobackend.infrastructure.persistence.jpa.adapter

import com.quno.qunobackend.domain.question.QuestionViewer
import com.quno.qunobackend.domain.tag.TagContributor
import com.quno.qunobackend.domain.tag.TagQuestionSort
import com.quno.qunobackend.domain.tag.TagStatsRepository
import com.quno.qunobackend.infrastructure.persistence.jpa.repository.TagStatsJpaRepository
import org.springframework.stereotype.Component

@Component
class TagStatsRepositoryAdapter(
    private val tagStatsJpaRepository: TagStatsJpaRepository,
) : TagStatsRepository {

    override fun findQuestionIds(tagId: Long, sort: TagQuestionSort, limit: Int, viewer: QuestionViewer): List<Long> = when (sort) {
        TagQuestionSort.LATEST -> tagStatsJpaRepository.findLatestQuestionIds(tagId, limit, viewer.userIdForSql(), viewer.organizationIdsForSql())
        TagQuestionSort.UNANSWERED -> tagStatsJpaRepository.findUnansweredQuestionIds(tagId, limit, viewer.userIdForSql(), viewer.organizationIdsForSql())
        TagQuestionSort.TOP -> tagStatsJpaRepository.findTopQuestionIds(tagId, limit, viewer.userIdForSql(), viewer.organizationIdsForSql())
    }

    override fun findTopContributors(tagId: Long, limit: Int): List<TagContributor> = tagStatsJpaRepository.findTopContributors(tagId, limit)
        .map { TagContributor(userId = it.getUserId(), nickname = it.getNickname(), answerCount = it.getAnswerCount()) }

    override fun findRelatedTagIds(tagId: Long, limit: Int): List<Long> = tagStatsJpaRepository.findRelatedTagIds(tagId, limit)
}
