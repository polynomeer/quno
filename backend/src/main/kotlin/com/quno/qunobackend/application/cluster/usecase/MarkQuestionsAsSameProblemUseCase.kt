package com.quno.qunobackend.application.cluster.usecase

import com.quno.qunobackend.application.cluster.dto.ClusterResult
import com.quno.qunobackend.domain.cluster.CannotClusterWithSelfException
import com.quno.qunobackend.domain.cluster.ClusterScopeMismatchException
import com.quno.qunobackend.domain.cluster.QuestionCluster
import com.quno.qunobackend.domain.cluster.QuestionClusterRepository
import com.quno.qunobackend.domain.question.QuestionNotFoundException
import com.quno.qunobackend.domain.question.QuestionRepository
import com.quno.qunobackend.domain.question.QuestionViewer
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Marks two questions as "the same problem" (see docs/architecture/decisions/0016) — the only
 * way a Cluster is formed or grows. If both already belong to different established clusters,
 * this now merges them (Phase 18, ADR-0030) — `related`'s cluster is absorbed into `question`'s,
 * which survives. Any Super Answer already designated on the absorbed cluster is not carried
 * over; a person can re-designate one after the merge if needed.
 */
@Service
class MarkQuestionsAsSameProblemUseCase(
    private val questionRepository: QuestionRepository,
    private val questionClusterRepository: QuestionClusterRepository,
) {
    @Transactional
    fun execute(questionId: Long, relatedQuestionId: Long, viewer: QuestionViewer): ClusterResult {
        if (questionId == relatedQuestionId) throw CannotClusterWithSelfException(questionId)
        val question = questionRepository.findById(questionId)?.takeIf(viewer::canSee) ?: throw QuestionNotFoundException(questionId)
        // The related id comes from the request body, so the URL gate never saw it (ADR-0085).
        val related = questionRepository.findById(relatedQuestionId)?.takeIf(viewer::canSee) ?: throw QuestionNotFoundException(relatedQuestionId)
        // A cluster shares its super answer with every member, so it never mixes scopes: all PUBLIC,
        // or all of one organization. Checking these two is enough — every cluster already holds that.
        if (question.visibility != related.visibility || question.organizationId != related.organizationId) {
            throw ClusterScopeMismatchException(questionId, relatedQuestionId)
        }

        val clusterId = when {
            question.clusterId != null && question.clusterId == related.clusterId -> question.clusterId
            question.clusterId != null && related.clusterId != null -> {
                val absorbedClusterId = related.clusterId
                val survivingClusterId = question.clusterId
                questionRepository.findAllByClusterId(absorbedClusterId)
                    .filter { it.id != related.id }
                    .forEach { questionRepository.save(it.joinCluster(survivingClusterId)) }
                questionClusterRepository.delete(absorbedClusterId)
                survivingClusterId
            }
            question.clusterId != null -> question.clusterId
            related.clusterId != null -> related.clusterId
            else -> requireNotNull(questionClusterRepository.save(QuestionCluster.create()).id)
        }

        if (question.clusterId != clusterId) questionRepository.save(question.joinCluster(clusterId))
        if (related.clusterId != clusterId) questionRepository.save(related.joinCluster(clusterId))

        val cluster = questionClusterRepository.findById(clusterId)
        val memberIds = questionRepository.findAllByClusterId(clusterId).mapNotNull { it.id }
        return ClusterResult(clusterId = clusterId, memberQuestionIds = memberIds, representativeAnswerId = cluster?.representativeAnswerId)
    }
}
