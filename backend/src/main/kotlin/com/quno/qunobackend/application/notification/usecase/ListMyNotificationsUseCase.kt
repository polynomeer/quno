package com.quno.qunobackend.application.notification.usecase

import com.quno.qunobackend.application.notification.dto.NotificationResult
import com.quno.qunobackend.domain.notification.Notification
import com.quno.qunobackend.domain.notification.NotificationRepository
import com.quno.qunobackend.domain.question.QuestionRepository
import com.quno.qunobackend.domain.question.QuestionViewer
import org.springframework.stereotype.Service

@Service
class ListMyNotificationsUseCase(
    private val notificationRepository: NotificationRepository,
    /** Null only in unit tests; Spring always injects it. */
    private val questionRepository: QuestionRepository? = null,
) {
    /** Drops notifications about questions [viewer] can no longer see — e.g. after leaving the
     * organization (ADR-0085). [viewer] defaults to "no organizations", which only hides more. */
    fun execute(userId: Long, viewer: QuestionViewer = QuestionViewer(userId, emptySet())): List<NotificationResult> {
        val notifications = notificationRepository.findAllByUserId(userId)
        val repository = questionRepository ?: return notifications.map { it.toResult() }
        val questionIds = notifications.mapNotNull { it.questionId }.distinct()
        val visible = repository.findAllByIds(questionIds).filter(viewer::canSee).mapNotNull { it.id }.toSet()
        return notifications.filter { it.questionId == null || it.questionId in visible }.map { it.toResult() }
    }
}

private fun Notification.toResult() = NotificationResult(
    id = requireNotNull(id),
    type = type,
    questionId = questionId,
    answerId = answerId,
    payload = payload,
    isRead = isRead,
    createdAt = createdAt,
)
