package com.quno.qunobackend.application.question.usecase

import com.quno.qunobackend.application.answer.usecase.InMemoryAnswerRepository
import com.quno.qunobackend.application.question.dto.QuestionTimelineEventType
import com.quno.qunobackend.application.review.usecase.InMemoryReviewRequestRepository
import com.quno.qunobackend.domain.answer.Answer
import com.quno.qunobackend.domain.question.Question
import com.quno.qunobackend.domain.question.QuestionNotFoundException
import com.quno.qunobackend.domain.question.QuestionVersion
import com.quno.qunobackend.domain.review.ReviewRequest
import com.quno.qunobackend.domain.review.ReviewRequestStatus
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GetQuestionTimelineUseCaseTest {
    private val questionRepository = InMemoryQuestionRepository()
    private val questionVersionRepository = InMemoryQuestionVersionRepository()
    private val answerRepository = InMemoryAnswerRepository()
    private val reviewRequestRepository = InMemoryReviewRequestRepository()
    private val useCase = GetQuestionTimelineUseCase(questionRepository, questionVersionRepository, answerRepository, reviewRequestRepository)

    private val t0 = Instant.parse("2026-10-01T00:00:00Z")

    private fun version(id: Long, questionId: Long, number: Int, hoursAfter: Long) = questionVersionRepository.save(
        QuestionVersion.reconstitute(
            id = id,
            questionId = questionId,
            versionNumber = number,
            title = "title",
            bodyMarkdown = "body",
            environment = null,
            logs = null,
            createdBy = 1L,
            createdAt = t0.plusSeconds(hoursAfter * 3600),
        ),
    )

    @Test
    fun `merges versions, answers and review requests newest first`() {
        val questionId = requireNotNull(questionRepository.save(Question.open(1L, "title")).id)
        version(id = 10L, questionId = questionId, number = 1, hoursAfter = 0)
        version(id = 11L, questionId = questionId, number = 2, hoursAfter = 3)
        answerRepository.save(
            Answer.reconstitute(
                id = 20L, questionId = questionId, authorId = 2L, bodyMarkdown = "a", isAccepted = true,
                targetVersionNumber = 1, latestVersionId = null, deletedAt = null,
                createdAt = t0.plusSeconds(1 * 3600), updatedAt = t0.plusSeconds(5 * 3600),
            ),
        )
        reviewRequestRepository.save(
            ReviewRequest.reconstitute(
                id = 30L,
                questionId = questionId,
                requestedBy = 3L,
                message = "EXPLAIN 결과를 붙여주세요",
                status = ReviewRequestStatus.ADDRESSED,
                questionVersionNumberAtRequest = 1,
                createdAt = t0.plusSeconds(2 * 3600),
                addressedAt = t0.plusSeconds(3 * 3600 + 1),
            ),
        )

        val events = useCase.execute(questionId)

        assertEquals(
            listOf(
                QuestionTimelineEventType.REVIEW_ADDRESSED,
                QuestionTimelineEventType.QUESTION_REVISED,
                QuestionTimelineEventType.REVIEW_REQUESTED,
                QuestionTimelineEventType.ANSWER_POSTED,
                QuestionTimelineEventType.QUESTION_CREATED,
            ),
            events.map { it.type },
        )
        val answer = events.single { it.type == QuestionTimelineEventType.ANSWER_POSTED }
        assertTrue(answer.accepted)
        assertEquals(20L, answer.answerId)
        // Acceptance carries no timestamp of its own — the event sits at the answer's creation time.
        assertEquals(t0.plusSeconds(3600), answer.occurredAt)
        assertNull(events.first().actorId)
    }

    @Test
    fun `throws for an unknown question`() {
        assertThrows<QuestionNotFoundException> { useCase.execute(999L) }
    }
}
