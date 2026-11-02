package com.quno.qunobackend.application.directask.usecase

import com.quno.qunobackend.application.directask.dto.DirectAskRequestListItemResult
import com.quno.qunobackend.domain.directask.DirectAskRequest
import com.quno.qunobackend.domain.directask.DirectAskRequestRepository
import com.quno.qunobackend.domain.directask.DirectAskRequestStatus
import com.quno.qunobackend.domain.question.QuestionRepository
import com.quno.qunobackend.domain.question.QuestionViewer
import com.quno.qunobackend.domain.user.UserRepository
import org.springframework.stereotype.Service

@Service
class ListMyDirectAsksUseCase(
    private val directAskRequestRepository: DirectAskRequestRepository,
    private val questionRepository: QuestionRepository,
    private val userRepository: UserRepository,
) {
    /** Includes AWAITING_PAYMENT — the requester should be able to see (and resume) a request
     * they haven't finished paying for. */
    fun executeSent(userId: Long, viewer: QuestionViewer = QuestionViewer(userId, emptySet())): List<DirectAskRequestListItemResult> = directAskRequestRepository.findAllByRequesterId(userId).mapNotNull { it.toListItem(viewer) }

    /** Excludes AWAITING_PAYMENT (Phase 25, ADR-0037) — the target only ever sees a request once
     * payment is confirmed, same invariant [ConfirmDirectAskPaymentUseCase] enforces for
     * notifications. */
    fun executeReceived(userId: Long, viewer: QuestionViewer = QuestionViewer(userId, emptySet())): List<DirectAskRequestListItemResult> = directAskRequestRepository.findAllByTargetUserId(userId)
        .filter { it.status != DirectAskRequestStatus.AWAITING_PAYMENT }
        .mapNotNull { it.toListItem(viewer) }

    /** Shows the title only while [viewer] may see the question (ADR-0085). */
    private fun DirectAskRequest.toListItem(viewer: QuestionViewer): DirectAskRequestListItemResult? {
        val question = questionRepository.findById(questionId)?.takeIf(viewer::canSee) ?: return null
        val requester = userRepository.findById(requesterId) ?: return null
        val target = userRepository.findById(targetUserId) ?: return null
        return DirectAskRequestListItemResult(
            id = requireNotNull(id),
            questionId = questionId,
            questionTitle = question.title,
            requesterId = requesterId,
            requesterNickname = requester.nickname,
            targetUserId = targetUserId,
            targetUserNickname = target.nickname,
            message = message,
            status = status,
            createdAt = createdAt,
            respondedAt = respondedAt,
        )
    }
}
