package com.quno.qunobackend.application.cluster.usecase

import com.quno.qunobackend.application.cluster.dto.ClusterDetailResult
import com.quno.qunobackend.application.common.QuestionSummaryHydrator
import com.quno.qunobackend.domain.cluster.ClusterNotFoundException
import com.quno.qunobackend.domain.cluster.QuestionClusterRepository
import com.quno.qunobackend.domain.cluster.QuestionNotInAnyClusterException
import com.quno.qunobackend.domain.question.QuestionNotFoundException
import com.quno.qunobackend.domain.question.QuestionRepository
import org.springframework.stereotype.Service

@Service
class GetClusterUseCase(
    private val questionClusterRepository: QuestionClusterRepository,
    private val questionRepository: QuestionRepository,
    private val hydrator: QuestionSummaryHydrator,
) {
    fun execute(clusterId: Long): ClusterDetailResult {
        val cluster = questionClusterRepository.findById(clusterId) ?: throw ClusterNotFoundException(clusterId)
        val memberIds = questionRepository.findAllByClusterId(clusterId).mapNotNull { it.id }
        return ClusterDetailResult(clusterId, hydrator.hydrate(memberIds), cluster.representativeAnswerId)
    }

    fun executeForQuestion(questionId: Long): ClusterDetailResult {
        val question = questionRepository.findById(questionId) ?: throw QuestionNotFoundException(questionId)
        val clusterId = question.clusterId ?: throw QuestionNotInAnyClusterException(questionId)
        return execute(clusterId)
    }
}
