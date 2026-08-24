package com.quno.qunobackend.domain.answer

import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AnswerTest {

    @Test
    fun `write creates an unaccepted answer`() {
        val answer = Answer.write(questionId = 1L, authorId = 2L, bodyMarkdown = "Try increasing the pool size.")

        assertFalse(answer.isAccepted)
        assertEquals(1L, answer.questionId)
    }

    @Test
    fun `write rejects a blank body`() {
        assertFailsWith<IllegalArgumentException> {
            Answer.write(questionId = 1L, authorId = 2L, bodyMarkdown = " ")
        }
    }

    @Test
    fun `accept marks the answer accepted`() {
        val answer = Answer.write(questionId = 1L, authorId = 2L, bodyMarkdown = "body")

        assertTrue(answer.accept().isAccepted)
    }

    @Test
    fun `accept refuses a deleted answer`() {
        val deleted = Answer.reconstitute(
            id = 1L, questionId = 1L, authorId = 2L, bodyMarkdown = "body", isAccepted = false,
            deletedAt = Instant.now(), createdAt = Instant.now(), updatedAt = Instant.now(),
        )

        assertFailsWith<IllegalStateException> { deleted.accept() }
    }

    @Test
    fun `unaccept clears the accepted flag`() {
        val accepted = Answer.write(questionId = 1L, authorId = 2L, bodyMarkdown = "body").accept()

        assertFalse(accepted.unaccept().isAccepted)
    }
}
