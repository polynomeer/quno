package com.quno.qunobackend.application.cluster.usecase

import com.quno.qunobackend.application.cluster.dto.QuestionGraphResult
import com.quno.qunobackend.application.common.QuestionSummaryHydrator
import com.quno.qunobackend.application.search.usecase.QuestionSearchUseCase
import com.quno.qunobackend.domain.question.QuestionNotFoundException
import com.quno.qunobackend.domain.question.QuestionRepository
import org.springframework.stereotype.Service

/**
 * Read-only "knowledge graph" view for one question — Cluster membership, Fork lineage, and
 * Related Questions in one response. Deliberately just data: no new computation or storage, and
 * no visualization (Phase 18, ADR-0030) — an actual node-edge diagram is a separate frontend
 * investment this Phase doesn't include.
 */
@Service
class GetQuestionGraphUseCase(
    private val questionRepository: QuestionRepository,
    private val hydrator: QuestionSummaryHydrator,
    private val questionSearchUseCase: QuestionSearchUseCase,
) {
    fun execute(questionId: Long): QuestionGraphResult {
        val question = questionRepository.findById(questionId) ?: throw QuestionNotFoundException(questionId)

        val clusterMembers = question.clusterId?.let { clusterId ->
            val memberIds = questionRepository.findAllByClusterId(clusterId).mapNotNull { it.id }.filter { it != questionId }
            hydrator.hydrate(memberIds)
        } ?: emptyList()

        val forkedFrom = question.originQuestionId?.let { originId -> hydrator.hydrate(listOf(originId)).firstOrNull() }

        val forkIds = questionRepository.findAllByOriginQuestionId(questionId).mapNotNull { it.id }
        val forks = hydrator.hydrate(forkIds)

        val related = questionSearchUseCase.related(questionId)

        return QuestionGraphResult(
            questionId = questionId,
            clusterMembers = clusterMembers,
            forkedFrom = forkedFrom,
            forks = forks,
            relatedQuestions = related,
        )
    }
}
