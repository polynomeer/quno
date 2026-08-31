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
        val answer = Answer.write(questionId = 1L, authorId = 2L, bodyMarkdown = "Try increasing the pool size.", targetVersionNumber = 1)

        assertFalse(answer.isAccepted)
        assertEquals(1L, answer.questionId)
        assertEquals(1, answer.targetVersionNumber)
    }

    @Test
    fun `write rejects a blank body`() {
        assertFailsWith<IllegalArgumentException> {
            Answer.write(questionId = 1L, authorId = 2L, bodyMarkdown = " ", targetVersionNumber = 1)
        }
    }

    @Test
    fun `write rejects a non-positive target version number`() {
        assertFailsWith<IllegalArgumentException> {
            Answer.write(questionId = 1L, authorId = 2L, bodyMarkdown = "body", targetVersionNumber = 0)
        }
    }

    @Test
    fun `accept marks the answer accepted`() {
        val answer = Answer.write(questionId = 1L, authorId = 2L, bodyMarkdown = "body", targetVersionNumber = 1)

        assertTrue(answer.accept().isAccepted)
    }

    @Test
    fun `accept refuses a deleted answer`() {
        val deleted = Answer.reconstitute(
            id = 1L, questionId = 1L, authorId = 2L, bodyMarkdown = "body", isAccepted = false, targetVersionNumber = 1,
            latestVersionId = 1L, deletedAt = Instant.now(), createdAt = Instant.now(), updatedAt = Instant.now(),
        )

        assertFailsWith<IllegalStateException> { deleted.accept() }
    }

    @Test
    fun `unaccept clears the accepted flag`() {
        val accepted = Answer.write(questionId = 1L, authorId = 2L, bodyMarkdown = "body", targetVersionNumber = 1).accept()

        assertFalse(accepted.unaccept().isAccepted)
    }

    @Test
    fun `a freshly written answer has no latest version yet`() {
        val answer = Answer.write(questionId = 1L, authorId = 2L, bodyMarkdown = "body", targetVersionNumber = 1)

        assertEquals(null, answer.latestVersionId)
    }

    @Test
    fun `withLatestVersion wires the pointer and refreshes the cached body`() {
        val answer = Answer.write(questionId = 1L, authorId = 2L, bodyMarkdown = "v1 body", targetVersionNumber = 1)

        val revised = answer.withLatestVersion(versionId = 42L, bodyMarkdown = "v2 body")

        assertEquals(42L, revised.latestVersionId)
        assertEquals("v2 body", revised.bodyMarkdown)
    }
}
