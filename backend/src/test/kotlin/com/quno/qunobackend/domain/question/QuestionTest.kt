package com.quno.qunobackend.domain.question

import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class QuestionTest {

    @Test
    fun `open creates a question with OPEN status and no version yet`() {
        val question = Question.open(authorId = 1L, title = "Redis timeout")

        assertEquals(QuestionStatus.OPEN, question.status)
        assertNull(question.latestVersionId)
        assertNull(question.id)
    }

    @Test
    fun `open rejects a blank title`() {
        assertFailsWith<IllegalArgumentException> {
            Question.open(authorId = 1L, title = " ")
        }
    }

    @Test
    fun `withLatestVersion points the question at the new version`() {
        val now = Instant.now()
        val question = Question.reconstitute(
            id = 1L,
            authorId = 1L,
            title = "Redis timeout",
            status = QuestionStatus.OPEN,
            latestVersionId = null,
            acceptedAnswerId = null,
            deletedAt = null,
            createdAt = now,
            updatedAt = now,
        )

        val updated = question.withLatestVersion(versionId = 42L)

        assertEquals(42L, updated.latestVersionId)
    }
}
