package com.quno.qunobackend.application.question.usecase

import com.quno.qunobackend.domain.question.Question
import com.quno.qunobackend.domain.question.QuestionNotFoundException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RecordQuestionViewUseCaseTest {
    private val questionRepository = InMemoryQuestionRepository()
    private val viewCountRepository = InMemoryQuestionViewCountRepository()
    private val useCase = RecordQuestionViewUseCase(questionRepository, viewCountRepository, InMemoryQuestionViewDeduplicator())

    @Test
    fun `counts each distinct viewer once`() {
        val questionId = requireNotNull(questionRepository.save(Question.open(1L, "title")).id)

        assertTrue(useCase.execute(questionId, "u:1"))
        assertFalse(useCase.execute(questionId, "u:1"))
        assertTrue(useCase.execute(questionId, "a:deadbeef"))

        assertEquals(2L, viewCountRepository.countByQuestionId(questionId))
    }

    @Test
    fun `throws for an unknown question without counting`() {
        assertThrows<QuestionNotFoundException> { useCase.execute(999L, "u:1") }
        assertEquals(0L, viewCountRepository.countByQuestionId(999L))
    }
}
