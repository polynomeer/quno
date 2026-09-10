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
    // 인기/재활성화 질문 두 섹션은 findById를 항목마다 호출하고 있었다(quality-improvement-plan.md
    // Q-2) — `/api/v1/flow`는 기본 limit=5*여러 섹션이라 방문할 때마다 실제로 여러 번 나가던
    // 쿼리였다. Phase 35에서 만든 배치 조회(findAllByIds)를 재사용해 섹션당 1번으로 줄인다.
    // 클러스터/Super Answer 섹션(cluster → answer → question 3단 조회)은 배치화하려면 두 리포지토리에
    // 새 배치 메서드가 필요하고, 이 섹션은 발생 빈도 자체가 낮아 이번에는 그대로 둔다.
    fun execute(limitPerSection: Int): List<FlowCard> {
        val cards = mutableListOf<FlowCard>()

        val popularIds = dashboardRepository.findPopularQuestionIds(limitPerSection)
        val popularQuestionsById = questionRepository.findAllByIds(popularIds).associateBy { it.id }
        popularIds.forEach { questionId ->
            val question = popularQuestionsById[questionId] ?: return@forEach
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

        val reopenedIds = flowRepository.findRecentlyReopenedQuestionIds(limitPerSection)
        val reopenedQuestionsById = questionRepository.findAllByIds(reopenedIds).associateBy { it.id }
        reopenedIds.forEach { questionId ->
            val question = reopenedQuestionsById[questionId] ?: return@forEach
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
