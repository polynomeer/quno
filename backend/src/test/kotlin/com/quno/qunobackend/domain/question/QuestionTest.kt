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

    @Test
    fun `revise moves an OPEN question to UPDATED`() {
        val question = openQuestion(status = QuestionStatus.OPEN)

        val revised = question.revise(versionId = 2L, title = "new title")

        assertEquals(QuestionStatus.UPDATED, revised.status)
        assertEquals("new title", revised.title)
        assertEquals(2L, revised.latestVersionId)
    }

    @Test
    fun `revise leaves a RESOLVED question RESOLVED`() {
        val question = openQuestion(status = QuestionStatus.RESOLVED)

        val revised = question.revise(versionId = 2L, title = "new title")

        assertEquals(QuestionStatus.RESOLVED, revised.status)
    }

    @Test
    fun `resolve sets RESOLVED status and the accepted answer id`() {
        val question = openQuestion(status = QuestionStatus.UPDATED)

        val resolved = question.resolve(acceptedAnswerId = 99L)

        assertEquals(QuestionStatus.RESOLVED, resolved.status)
        assertEquals(99L, resolved.acceptedAnswerId)
    }

    @Test
    fun `requestMoreInfo moves an OPEN question to NEEDS_INFO`() {
        val question = openQuestion(status = QuestionStatus.OPEN)

        assertEquals(QuestionStatus.NEEDS_INFO, question.requestMoreInfo().status)
    }

    @Test
    fun `requestMoreInfo on an already NEEDS_INFO question is a no-op`() {
        val question = openQuestion(status = QuestionStatus.NEEDS_INFO)

        assertEquals(QuestionStatus.NEEDS_INFO, question.requestMoreInfo().status)
    }

    @Test
    fun `requestMoreInfo rejects a resolved question`() {
        val question = openQuestion(status = QuestionStatus.RESOLVED)

        assertFailsWith<QuestionAlreadyResolvedException> { question.requestMoreInfo() }
    }

    private fun openQuestion(status: QuestionStatus): Question {
        val now = Instant.now()
        return Question.reconstitute(
            id = 1L,
            authorId = 1L,
            title = "Redis timeout",
            status = status,
            latestVersionId = 1L,
            acceptedAnswerId = null,
            deletedAt = null,
            createdAt = now,
            updatedAt = now,
        )
    }
}
