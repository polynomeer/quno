package com.quno.qunobackend.application.common

import com.quno.qunobackend.application.search.dto.QuestionSearchResult
import com.quno.qunobackend.domain.question.QuestionRepository
import com.quno.qunobackend.domain.tag.QuestionTagRepository
import com.quno.qunobackend.domain.vote.VoteRepository
import com.quno.qunobackend.domain.vote.VoteTargetType
import org.springframework.stereotype.Component

/**
 * Turns ranked question ids (from Search, Related, or Recommendation) into display summaries.
 * Shared because search/related/recommend are all "rank candidate ids, then hydrate" — only
 * the ranking strategy differs between them. Also where `score` (Phase 11) gets attached, so
 * every consumer (search, dashboard, related, cluster members) picks it up from one place.
 */
@Component
class QuestionSummaryHydrator(
    private val questionRepository: QuestionRepository,
    private val questionTagRepository: QuestionTagRepository,
    private val voteRepository: VoteRepository,
) {
    /** Silently drops ids that no longer resolve to a question (e.g. deleted since ranking ran).
     * Batches all three lookups instead of querying per id — `ids` is often a ranked list
     * (search/related/recommend), so this used to be 3*N queries for N results. */
    fun hydrate(ids: List<Long>): List<QuestionSearchResult> {
        if (ids.isEmpty()) return emptyList()

        val questionsById = questionRepository.findAllByIds(ids).associateBy { requireNotNull(it.id) }
        val tagsByQuestionId = questionTagRepository.findTagsByQuestionIds(ids)
        val scoresByQuestionId = voteRepository.sumScoresByTargets(VoteTargetType.QUESTION, ids)

        return ids.mapNotNull { id ->
            val question = questionsById[id] ?: return@mapNotNull null
            QuestionSearchResult(
                id = id,
                title = question.title,
                status = question.status,
                tags = tagsByQuestionId[id].orEmpty().map { it.name },
                score = scoresByQuestionId[id] ?: 0L,
            )
        }
    }
}
