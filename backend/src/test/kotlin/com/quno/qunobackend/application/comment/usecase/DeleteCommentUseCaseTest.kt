package com.quno.qunobackend.application.comment.usecase

import com.quno.qunobackend.application.answer.usecase.InMemoryAnswerRepository
import com.quno.qunobackend.application.comment.dto.CreateCommentCommand
import com.quno.qunobackend.application.common.InMemoryOutboxEventRepository
import com.quno.qunobackend.application.question.dto.CreateQuestionCommand
import com.quno.qunobackend.application.question.usecase.CreateQuestionUseCase
import com.quno.qunobackend.application.question.usecase.InMemoryQuestionRepository
import com.quno.qunobackend.application.question.usecase.InMemoryQuestionVersionRepository
import com.quno.qunobackend.application.tag.usecase.InMemoryQuestionTagRepository
import com.quno.qunobackend.application.tag.usecase.InMemoryTagRepository
import com.quno.qunobackend.domain.comment.CommentAccessDeniedException
import com.quno.qunobackend.domain.comment.CommentNotFoundException
import com.quno.qunobackend.domain.comment.CommentTargetType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class DeleteCommentUseCaseTest {
    private val questionRepository = InMemoryQuestionRepository()
    private val tagRepository = InMemoryTagRepository()
    private val createQuestionUseCase = CreateQuestionUseCase(
        questionRepository, InMemoryQuestionVersionRepository(), tagRepository, InMemoryQuestionTagRepository(tagRepository),
    )
    private val commentRepository = InMemoryCommentRepository()
    private val createCommentUseCase = CreateCommentUseCase(
        questionRepository, InMemoryAnswerRepository(), commentRepository, InMemoryOutboxEventRepository(),
    )
    private val deleteCommentUseCase = DeleteCommentUseCase(commentRepository)

    private fun aComment(authorId: Long = 2L): Long {
        val questionId = createQuestionUseCase.execute(
            CreateQuestionCommand(authorId = 1L, title = "t", body = "body", environment = null, logs = null),
        ).id
        return createCommentUseCase.execute(
            CreateCommentCommand(CommentTargetType.QUESTION, questionId, authorId = authorId, body = "a comment"),
        ).id
    }

    @Test
    fun `the author can delete their own comment, tombstoning it`() {
        val commentId = aComment(authorId = 2L)

        deleteCommentUseCase.execute(commentId, actorId = 2L)

        val comment = commentRepository.findById(commentId)!!
        assertNotNull(comment.deletedAt)
        assertNull(comment.toResult().body)
        assertEquals(true, comment.toResult().isDeleted)
    }

    @Test
    fun `deleting an already-deleted comment stays idempotent`() {
        val commentId = aComment(authorId = 2L)
        deleteCommentUseCase.execute(commentId, actorId = 2L)

        deleteCommentUseCase.execute(commentId, actorId = 2L)

        assertNotNull(commentRepository.findById(commentId)!!.deletedAt)
    }

    @Test
    fun `rejects deletion by someone other than the author`() {
        val commentId = aComment(authorId = 2L)

        assertFailsWith<CommentAccessDeniedException> {
            deleteCommentUseCase.execute(commentId, actorId = 999L)
        }
    }

    @Test
    fun `rejects deleting a comment that does not exist`() {
        assertFailsWith<CommentNotFoundException> {
            deleteCommentUseCase.execute(999L, actorId = 2L)
        }
    }
}
