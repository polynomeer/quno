package com.quno.qunobackend.domain.question

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class QuestionViewerTest {
    private val private = Question.open(authorId = 1L, title = "t", visibility = QuestionVisibility.ORG, organizationId = 10L)
    private val public = Question.open(authorId = 1L, title = "t")

    @Test
    fun `members, the author and anyone for public questions`() {
        assertTrue(QuestionViewer(2L, setOf(10L)).canSee(private))
        assertTrue(QuestionViewer(1L, emptySet()).canSee(private), "the author, even after leaving")
        assertFalse(QuestionViewer(3L, setOf(11L)).canSee(private))
        assertFalse(QuestionViewer.ANONYMOUS.canSee(private))
        assertTrue(QuestionViewer.ANONYMOUS.canSee(public))
    }

    @Test
    fun `sql parameters are never empty and never match a real row by accident`() {
        assertEquals(listOf(-1L), QuestionViewer.ANONYMOUS.organizationIdsForSql())
        assertEquals(-1L, QuestionViewer.ANONYMOUS.userIdForSql())
        assertEquals(listOf(10L), QuestionViewer(2L, setOf(10L)).organizationIdsForSql())
    }

    @Test
    fun `an ORG question always names its organization and a PUBLIC one never does`() {
        assertThrows<IllegalArgumentException> { Question.open(authorId = 1L, title = "t", visibility = QuestionVisibility.ORG) }
        assertThrows<IllegalArgumentException> { Question.open(authorId = 1L, title = "t", organizationId = 10L) }
    }
}
