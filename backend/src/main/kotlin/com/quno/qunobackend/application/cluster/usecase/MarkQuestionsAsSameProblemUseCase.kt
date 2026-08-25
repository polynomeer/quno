package com.quno.qunobackend.application.cluster.usecase

import com.quno.qunobackend.application.cluster.dto.ClusterResult
import com.quno.qunobackend.domain.cluster.CannotClusterWithSelfException
import com.quno.qunobackend.domain.cluster.ClustersAlreadyDistinctException
import com.quno.qunobackend.domain.cluster.QuestionCluster
import com.quno.qunobackend.domain.cluster.QuestionClusterRepository
import com.quno.qunobackend.domain.question.QuestionNotFoundException
import com.quno.qunobackend.domain.question.QuestionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Marks two questions as "the same problem" (see docs/architecture/decisions/0016) — the only
 * way a Cluster is formed or grows. If both already belong to different established clusters,
 * this is rejected rather than merging them (that's the deferred Merge feature's job).
 */
@Service
class MarkQuestionsAsSameProblemUseCase(
    private val questionRepository: QuestionRepository,
    private val questionClusterRepository: QuestionClusterRepository,
) {
    @Transactional
    fun execute(questionId: Long, relatedQuestionId: Long): ClusterResult {
        if (questionId == relatedQuestionId) throw CannotClusterWithSelfException(questionId)
        val question = questionRepository.findById(questionId) ?: throw QuestionNotFoundException(questionId)
        val related = questionRepository.findById(relatedQuestionId) ?: throw QuestionNotFoundException(relatedQuestionId)

        val clusterId = when {
            question.clusterId != null && question.clusterId == related.clusterId -> question.clusterId
            question.clusterId != null && related.clusterId != null ->
                throw ClustersAlreadyDistinctException(question.clusterId, related.clusterId)
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
