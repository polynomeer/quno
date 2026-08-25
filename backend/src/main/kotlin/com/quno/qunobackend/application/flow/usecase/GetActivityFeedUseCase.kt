package com.quno.qunobackend.application.flow.usecase

import com.quno.qunobackend.domain.answer.AnswerRepository
import com.quno.qunobackend.domain.cluster.QuestionClusterRepository
import com.quno.qunobackend.domain.dashboard.DashboardRepository
import com.quno.qunobackend.domain.flow.FlowCard
import com.quno.qunobackend.domain.flow.FlowCardType
import com.quno.qunobackend.domain.flow.FlowRepository
import com.quno.qunobackend.domain.question.QuestionRepository
import com.quno.qunobackend.domain.qunobot.SpikeDetectionRepository
import org.springframework.stereotype.Service

/**
 * Assembles Quno Flow (PLAN.md 10.3) — a fixed-order sequence of card sections, each reusing an
 * existing signal rather than introducing new business logic: popular questions (Dashboard),
 * tag spikes (QunoBot), reopened questions and recently-super-answered clusters (Flow's own
 * derived queries, PLAN.md 10.1).
 */
@Service
class GetActivityFeedUseCase(
    private val dashboardRepository: DashboardRepository,
    private val spikeDetectionRepository: SpikeDetectionRepository,
    private val flowRepository: FlowRepository,
    private val questionRepository: QuestionRepository,
    private val answerRepository: AnswerRepository,
    private val questionClusterRepository: QuestionClusterRepository,
) {
    fun execute(limitPerSection: Int): List<FlowCard> {
        val cards = mutableListOf<FlowCard>()

        dashboardRepository.findPopularQuestionIds(limitPerSection).forEach { questionId ->
            val question = questionRepository.findById(questionId) ?: return@forEach
            cards += FlowCard(
                type = FlowCardType.POPULAR_QUESTION,
                headline = "\"${question.title}\"이(가) 지금 가장 인기 있는 질문입니다",
                questionId = questionId,
            )
        }

        spikeDetectionRepository.findSpikingTags(limitPerSection).forEach { spike ->
            cards += FlowCard(
                type = FlowCardType.TAG_SPIKE,
                headline = "${spike.name} 관련 질문이 평소보다 %.1f배 늘었습니다".format(spike.spikeRatio),
            )
        }

        flowRepository.findRecentlyReopenedQuestionIds(limitPerSection).forEach { questionId ->
            val question = questionRepository.findById(questionId) ?: return@forEach
            cards += FlowCard(
                type = FlowCardType.REOPENED_QUESTION,
                headline = "\"${question.title}\"이(가) 다시 활성화되었습니다",
                questionId = questionId,
            )
        }

        flowRepository.findRecentlySuperAnsweredClusterIds(limitPerSection).forEach { clusterId ->
            val cluster = questionClusterRepository.findById(clusterId) ?: return@forEach
            val answerId = cluster.representativeAnswerId ?: return@forEach
            val answer = answerRepository.findById(answerId) ?: return@forEach
            val question = questionRepository.findById(answer.questionId) ?: return@forEach
            cards += FlowCard(
                type = FlowCardType.CLUSTER_SUPER_ANSWER,
                headline = "\"${question.title}\" 클러스터에 새로운 Super Answer가 등록되었습니다",
                clusterId = clusterId,
            )
        }

        return cards
    }
}
