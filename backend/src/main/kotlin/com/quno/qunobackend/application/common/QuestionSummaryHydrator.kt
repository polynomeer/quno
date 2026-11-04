package com.quno.qunobackend.application.common

import com.quno.qunobackend.application.search.dto.QuestionSearchResult
import com.quno.qunobackend.domain.answer.AnswerRepository
import com.quno.qunobackend.domain.question.QuestionRepository
import com.quno.qunobackend.domain.question.QuestionVersionRepository
import com.quno.qunobackend.domain.question.QuestionViewCountRepository
import com.quno.qunobackend.domain.question.QuestionViewer
import com.quno.qunobackend.domain.tag.QuestionTagRepository
import com.quno.qunobackend.domain.verification.QuestionVerificationRepository
import com.quno.qunobackend.domain.verification.QuestionVerifier
import com.quno.qunobackend.domain.vote.VoteRepository
import com.quno.qunobackend.domain.vote.VoteTargetType
import org.springframework.stereotype.Component
import java.time.Instant

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
    private val answerRepository: AnswerRepository,
    private val questionVersionRepository: QuestionVersionRepository,
    private val questionViewCountRepository: QuestionViewCountRepository,
    /** Null only in unit tests that don't care about levels; Spring always injects it. */
    private val verificationRepository: QuestionVerificationRepository? = null,
) {
    /** Silently drops ids that no longer resolve to a question (e.g. deleted since ranking ran).
     * Batches every lookup instead of querying per id — `ids` is often a ranked list
     * (search/related/recommend), so this used to be 3*N queries for N results. Answer counts and
     * latest version numbers (ADR-0062) follow the same one-query-per-kind rule.
     *
     * [viewer] is required on purpose (Phase 79, ADR-0085): every ranked list passes through here,
     * so this is the backstop that drops questions the viewer may not see, whatever the ranking
     * query did. */
    fun hydrate(ids: List<Long>, viewer: QuestionViewer): List<QuestionSearchResult> {
        if (ids.isEmpty()) return emptyList()

        val questionsById = questionRepository.findAllByIds(ids).filter(viewer::canSee).associateBy { requireNotNull(it.id) }
        val tagsByQuestionId = questionTagRepository.findTagsByQuestionIds(ids)
        val scoresByQuestionId = voteRepository.sumScoresByTargets(VoteTargetType.QUESTION, ids)
        val answerCountsByQuestionId = answerRepository.countByQuestionIds(ids)
        val versionNumbersByQuestionId = questionVersionRepository.findLatestVersionNumbersByQuestionIds(ids)
        val viewCountsByQuestionId = questionViewCountRepository.countsByQuestionIds(ids)
        val verifiedLevels = verificationRepository?.findVerifiedLevels(ids, Instant.now().minus(QuestionVerifier.FRESH_FOR)).orEmpty()

        return ids.mapNotNull { id ->
            val question = questionsById[id] ?: return@mapNotNull null
            QuestionSearchResult(
                id = id,
                title = question.title,
                status = question.status,
                visibility = question.visibility,
                organizationId = question.organizationId,
                tags = tagsByQuestionId[id].orEmpty().map { it.name },
                score = scoresByQuestionId[id] ?: 0L,
                answerCount = answerCountsByQuestionId[id] ?: 0,
                hasAcceptedAnswer = question.acceptedAnswerId != null,
                versionNumber = versionNumbersByQuestionId[id] ?: 1,
                viewCount = viewCountsByQuestionId[id] ?: 0L,
                createdAt = question.createdAt,
                updatedAt = question.updatedAt,
                verifiedLevel = verifiedLevels[id],
            )
        }
    }
}
