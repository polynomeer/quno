package com.quno.qunobackend.application.review.usecase

import com.quno.qunobackend.application.common.InMemoryOutboxEventRepository
import com.quno.qunobackend.application.question.dto.CreateQuestionCommand
import com.quno.qunobackend.application.question.usecase.CreateQuestionUseCase
import com.quno.qunobackend.application.question.usecase.InMemoryQuestionRepository
import com.quno.qunobackend.application.question.usecase.InMemoryQuestionVersionRepository
import com.quno.qunobackend.application.review.dto.CreateReviewRequestCommand
import com.quno.qunobackend.application.tag.usecase.InMemoryQuestionTagRepository
import com.quno.qunobackend.application.tag.usecase.InMemoryTagRepository
import com.quno.qunobackend.domain.question.QuestionNotFoundException
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ListReviewRequestsUseCaseTest {
    private val questionRepository = InMemoryQuestionRepository()
    private val questionVersionRepository = InMemoryQuestionVersionRepository()
    private val tagRepository = InMemoryTagRepository()
    private val reviewRequestRepository = InMemoryReviewRequestRepository()

    private val createQuestionUseCase = CreateQuestionUseCase(
        questionRepository, questionVersionRepository, tagRepository, InMemoryQuestionTagRepository(tagRepository),
    )
    private val createReviewRequestUseCase = CreateReviewRequestUseCase(
        questionRepository, questionVersionRepository, reviewRequestRepository, InMemoryOutboxEventRepository(),
    )
    private val useCase = ListReviewRequestsUseCase(questionRepository, reviewRequestRepository)

    @Test
    fun `lists all review requests for a question, most recent first`() {
        val questionId = createQuestionUseCase.execute(
            CreateQuestionCommand(authorId = 1L, title = "t", body = "body", environment = null, logs = null),
        ).id
        createReviewRequestUseCase.execute(CreateReviewRequestCommand(questionId, requestedBy = 2L, message = "first"))
        createReviewRequestUseCase.execute(CreateReviewRequestCommand(questionId, requestedBy = 3L, message = "second"))

        val results = useCase.execute(questionId)

        assertEquals(listOf("second", "first"), results.map { it.message })
    }

    @Test
    fun `rejects listing for a question that does not exist`() {
        assertFailsWith<QuestionNotFoundException> { useCase.execute(999L) }
    }
}
