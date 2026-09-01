package com.quno.qunobackend.domain.tag

/** Latest = newest first. Unanswered = no non-deleted answers, newest first. Top = highest net
 * vote score first (Phase 28, ADR-0040) — mirrors SearchSort's RELEVANCE/SCORE split. */
enum class TagQuestionSort { LATEST, UNANSWERED, TOP }

data class TagContributor(val userId: Long, val nickname: String, val answerCount: Long)

/** Port implemented by infrastructure/persistence/jpa/adapter/TagStatsRepositoryAdapter. */
interface TagStatsRepository {
    fun findQuestionIds(tagId: Long, sort: TagQuestionSort, limit: Int): List<Long>

    /** Ranked by how many answers they've posted to questions carrying this tag. */
    fun findTopContributors(tagId: Long, limit: Int): List<TagContributor>

    /** Other tags most frequently co-occurring with this one across questions, most first. */
    fun findRelatedTagIds(tagId: Long, limit: Int): List<Long>
}
