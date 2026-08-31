package com.quno.qunobackend.application.comment.usecase

import com.quno.qunobackend.application.answer.usecase.InMemoryAnswerRepository
import com.quno.qunobackend.application.comment.dto.CreateCommentCommand
import com.quno.qunobackend.application.comment.dto.EditCommentCommand
import com.quno.qunobackend.application.common.InMemoryOutboxEventRepository
import com.quno.qunobackend.application.question.dto.CreateQuestionCommand
import com.quno.qunobackend.application.question.usecase.CreateQuestionUseCase
import com.quno.qunobackend.application.question.usecase.InMemoryQuestionRepository
import com.quno.qunobackend.application.question.usecase.InMemoryQuestionVersionRepository
import com.quno.qunobackend.application.tag.usecase.InMemoryQuestionTagRepository
import com.quno.qunobackend.application.tag.usecase.InMemoryTagRepository
import com.quno.qunobackend.application.user.usecase.InMemoryUserRepository
import com.quno.qunobackend.domain.comment.CommentAccessDeniedException
import com.quno.qunobackend.domain.comment.CommentAlreadyDeletedException
import com.quno.qunobackend.domain.comment.CommentTargetType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class EditCommentUseCaseTest {
    private val questionRepository = InMemoryQuestionRepository()
    private val tagRepository = InMemoryTagRepository()
    private val createQuestionUseCase = CreateQuestionUseCase(
        questionRepository, InMemoryQuestionVersionRepository(), tagRepository, InMemoryQuestionTagRepository(tagRepository),
    )
    private val commentRepository = InMemoryCommentRepository()
    private val commentVersionRepository = InMemoryCommentVersionRepository()
    private val createCommentUseCase = CreateCommentUseCase(
        questionRepository, InMemoryAnswerRepository(), commentRepository, InMemoryUserRepository(), InMemoryOutboxEventRepository(),
    )
    private val deleteCommentUseCase = DeleteCommentUseCase(commentRepository)
    private val useCase = EditCommentUseCase(commentRepository, commentVersionRepository)

    private fun aComment(authorId: Long = 2L): Long {
        val questionId = createQuestionUseCase.execute(
            CreateQuestionCommand(authorId = 1L, title = "t", body = "body", environment = null, logs = null),
        ).id
        return createCommentUseCase.execute(
            CreateCommentCommand(CommentTargetType.QUESTION, questionId, authorId = authorId, body = "original"),
        ).id
    }

    @Test
    fun `the author can edit their own comment, bumping the version and archiving the old body`() {
        val commentId = aComment(authorId = 2L)

        val result = useCase.execute(EditCommentCommand(commentId, editorId = 2L, body = "fixed typo"))

        assertEquals("fixed typo", result.body)
        assertEquals(2, result.versionNumber)
        val versions = commentVersionRepository.findAllByCommentIdOrderByVersionNumberAsc(commentId)
        assertEquals(listOf(1), versions.map { it.versionNumber })
        assertEquals("original", versions.single().body)
    }

    @Test
    fun `rejects editing by someone other than the author`() {
        val commentId = aComment(authorId = 2L)

        assertFailsWith<CommentAccessDeniedException> {
            useCase.execute(EditCommentCommand(commentId, editorId = 999L, body = "hijacked"))
        }
    }

    @Test
    fun `rejects editing an already-deleted comment`() {
        val commentId = aComment(authorId = 2L)
        deleteCommentUseCase.execute(commentId, actorId = 2L)

        assertFailsWith<CommentAlreadyDeletedException> {
            useCase.execute(EditCommentCommand(commentId, editorId = 2L, body = "too late"))
        }
    }

    @Test
    fun `rejects a blank edited body`() {
        val commentId = aComment(authorId = 2L)

        assertFailsWith<IllegalArgumentException> {
            useCase.execute(EditCommentCommand(commentId, editorId = 2L, body = "   "))
        }
    }
}
