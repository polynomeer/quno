package com.quno.qunobackend.application.question.usecase

import com.quno.qunobackend.application.question.dto.QuestionTimelineEventResult
import com.quno.qunobackend.application.question.dto.QuestionTimelineEventType
import com.quno.qunobackend.domain.answer.AnswerRepository
import com.quno.qunobackend.domain.question.QuestionNotFoundException
import com.quno.qunobackend.domain.question.QuestionRepository
import com.quno.qunobackend.domain.question.QuestionVersionRepository
import com.quno.qunobackend.domain.review.ReviewRequestRepository
import org.springframework.stereotype.Service

/**
 * "질문의 생애" panel (design.md #3.3, ADR-0062): only the events that changed what the question
 * means — creation, revisions, answers, QPR review requests — composed from aggregates that
 * already store their own timestamps. Comments are deliberately left out.
 *
 * Acceptance is a flag on the answer's event, not an event of its own: neither Answer nor
 * Question records *when* acceptance happened (both `updatedAt`s move again on later edits), so
 * a separate "accepted at" entry would have to invent its time.
 */
@Service
class GetQuestionTimelineUseCase(
    private val questionRepository: QuestionRepository,
    private val questionVersionRepository: QuestionVersionRepository,
    private val answerRepository: AnswerRepository,
    private val reviewRequestRepository: ReviewRequestRepository,
) {
    /** Newest first. */
    fun execute(questionId: Long): List<QuestionTimelineEventResult> {
        questionRepository.findById(questionId) ?: throw QuestionNotFoundException(questionId)

        val versionEvents = questionVersionRepository.findAllByQuestionIdOrderByVersionNumberAsc(questionId).map {
            QuestionTimelineEventResult(
                type = if (it.versionNumber == 1) QuestionTimelineEventType.QUESTION_CREATED else QuestionTimelineEventType.QUESTION_REVISED,
                occurredAt = it.createdAt,
                actorId = it.createdBy,
                versionNumber = it.versionNumber,
            )
        }
        val answerEvents = answerRepository.findAllByQuestionId(questionId).map {
            QuestionTimelineEventResult(
                type = QuestionTimelineEventType.ANSWER_POSTED,
                occurredAt = it.createdAt,
                actorId = it.authorId,
                versionNumber = it.targetVersionNumber,
                answerId = it.id,
                accepted = it.isAccepted,
            )
        }
        val reviewEvents = reviewRequestRepository.findAllByQuestionId(questionId).flatMap { request ->
            val requested = QuestionTimelineEventResult(
                type = QuestionTimelineEventType.REVIEW_REQUESTED,
                occurredAt = request.createdAt,
                actorId = request.requestedBy,
                versionNumber = request.questionVersionNumberAtRequest,
            )
            val addressed = request.addressedAt?.let {
                QuestionTimelineEventResult(type = QuestionTimelineEventType.REVIEW_ADDRESSED, occurredAt = it, actorId = null)
            }
            listOfNotNull(requested, addressed)
        }

        return (versionEvents + answerEvents + reviewEvents).sortedByDescending { it.occurredAt }
    }
}
