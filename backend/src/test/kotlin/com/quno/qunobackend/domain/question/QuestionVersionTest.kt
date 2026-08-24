package com.quno.qunobackend.domain.question

import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class QuestionVersionTest {

    @Test
    fun `create rejects a version number below 1`() {
        assertFailsWith<IllegalArgumentException> {
            QuestionVersion.create(
                questionId = 1L,
                versionNumber = 0,
                title = "Redis timeout",
                bodyMarkdown = "body",
                environment = null,
                logs = null,
                createdBy = 1L,
            )
        }
    }

    @Test
    fun `create rejects a blank body`() {
        assertFailsWith<IllegalArgumentException> {
            QuestionVersion.create(
                questionId = 1L,
                versionNumber = 1,
                title = "Redis timeout",
                bodyMarkdown = " ",
                environment = null,
                logs = null,
                createdBy = 1L,
            )
        }
    }
}
