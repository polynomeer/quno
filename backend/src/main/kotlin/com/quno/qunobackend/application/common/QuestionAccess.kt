package com.quno.qunobackend.application.common

import com.quno.qunobackend.domain.answer.AnswerRepository
import com.quno.qunobackend.domain.comment.CommentRepository
import com.quno.qunobackend.domain.comment.CommentTargetType
import com.quno.qunobackend.domain.directask.DirectAskRequestRepository
import com.quno.qunobackend.domain.engine.EngineSuggestionRepository
import com.quno.qunobackend.domain.livechat.LiveChatRoomRepository
import com.quno.qunobackend.domain.organization.OrganizationMembershipRepository
import com.quno.qunobackend.domain.question.Question
import com.quno.qunobackend.domain.question.QuestionNotFoundException
import com.quno.qunobackend.domain.question.QuestionRepository
import com.quno.qunobackend.domain.question.QuestionViewer
import org.springframework.stereotype.Component

/** Resources addressed by their own id whose visibility is their question's (ADR-0085). */
enum class QuestionScopedResource { QUESTION, ANSWER, COMMENT, LIVE_CHAT_ROOM, DIRECT_ASK, ENGINE_SUGGESTION }

/**
 * The one place that decides whether someone may see a question (Phase 79, ADR-0085). Everything
 * that shows a question — the request gate, list filters, notification fan-out — asks here.
 * An invisible question is reported as not found, never as forbidden: a 403 would confirm it exists.
 */
@Component
class QuestionAccess(
    private val membershipRepository: OrganizationMembershipRepository,
    private val questionRepository: QuestionRepository,
    private val answerRepository: AnswerRepository,
    private val commentRepository: CommentRepository,
    private val liveChatRoomRepository: LiveChatRoomRepository,
    private val directAskRequestRepository: DirectAskRequestRepository,
    private val engineSuggestionRepository: EngineSuggestionRepository,
) {
    fun viewer(userId: Long?): QuestionViewer = if (userId == null) QuestionViewer.ANONYMOUS else QuestionViewer(userId, membershipRepository.findOrganizationIdsByUserId(userId).toSet())

    /** The question, if [viewer] may see it; otherwise exactly what a missing question looks like. */
    fun requireVisible(questionId: Long, viewer: QuestionViewer): Question {
        val question = questionRepository.findById(questionId)
        if (question == null || !viewer.canSee(question)) throw QuestionNotFoundException(questionId)
        return question
    }

    fun canSee(questionId: Long, viewer: QuestionViewer): Boolean = questionRepository.findById(questionId)?.let(viewer::canSee) ?: false

    /** Which of [questionIds] [viewer] may see — one query, for filtering lists. */
    fun visibleIds(questionIds: Collection<Long>, viewer: QuestionViewer): Set<Long> {
        if (questionIds.isEmpty()) return emptySet()
        return questionRepository.findAllByIds(questionIds.distinct()).filter(viewer::canSee).mapNotNull { it.id }.toSet()
    }

    /** The question a resource belongs to, or null when the resource doesn't exist. */
    fun owningQuestionId(resource: QuestionScopedResource, id: Long): Long? = when (resource) {
        QuestionScopedResource.QUESTION -> id
        QuestionScopedResource.ANSWER -> answerRepository.findById(id)?.questionId
        QuestionScopedResource.COMMENT -> commentRepository.findById(id)?.let { comment ->
            when (comment.targetType) {
                CommentTargetType.QUESTION -> comment.targetId
                CommentTargetType.ANSWER -> answerRepository.findById(comment.targetId)?.questionId
            }
        }
        QuestionScopedResource.LIVE_CHAT_ROOM -> liveChatRoomRepository.findById(id)?.questionId
        QuestionScopedResource.DIRECT_ASK -> directAskRequestRepository.findById(id)?.questionId
        QuestionScopedResource.ENGINE_SUGGESTION -> engineSuggestionRepository.findById(id)?.questionId
    }

    /**
     * True when the resource's question is hidden from [viewer]. A resource that doesn't exist is
     * not "hidden" — the endpoint's own 404 handles it.
     */
    fun isHidden(resource: QuestionScopedResource, id: Long, viewer: QuestionViewer): Boolean {
        val questionId = owningQuestionId(resource, id) ?: return false
        val question = questionRepository.findById(questionId) ?: return false
        return !viewer.canSee(question)
    }
}
