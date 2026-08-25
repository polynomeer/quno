package com.quno.qunobackend.application.cluster.usecase

import com.quno.qunobackend.application.cluster.dto.ClusterDetailResult
import com.quno.qunobackend.domain.answer.AnswerNotFoundException
import com.quno.qunobackend.domain.answer.AnswerRepository
import com.quno.qunobackend.domain.cluster.AnswerNotAcceptedException
import com.quno.qunobackend.domain.cluster.AnswerNotInClusterException
import com.quno.qunobackend.domain.cluster.ClusterNotFoundException
import com.quno.qunobackend.domain.cluster.QuestionClusterRepository
import com.quno.qunobackend.domain.question.QuestionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Explicitly designates a cluster's "Super Answer" (vision.md) — no automatic scoring, matching
 * the manual, human-curated approach chosen for Cluster itself (ADR-0016). Anyone authenticated
 * can designate one, same permission model as ReviewRequest (no role/reputation system yet).
 */
@Service
class DesignateSuperAnswerUseCase(
    private val questionClusterRepository: QuestionClusterRepository,
    private val questionRepository: QuestionRepository,
    private val answerRepository: AnswerRepository,
    private val getClusterUseCase: GetClusterUseCase,
) {
    @Transactional
    fun execute(clusterId: Long, answerId: Long): ClusterDetailResult {
        val cluster = questionClusterRepository.findById(clusterId) ?: throw ClusterNotFoundException(clusterId)
        val answer = answerRepository.findById(answerId) ?: throw AnswerNotFoundException(answerId)
        if (!answer.isAccepted) throw AnswerNotAcceptedException(answerId)

        val memberQuestionIds = questionRepository.findAllByClusterId(clusterId).mapNotNull { it.id }
        if (answer.questionId !in memberQuestionIds) throw AnswerNotInClusterException(answerId, clusterId)

        questionClusterRepository.save(cluster.designateSuperAnswer(answerId))
        return getClusterUseCase.execute(clusterId)
    }
}
