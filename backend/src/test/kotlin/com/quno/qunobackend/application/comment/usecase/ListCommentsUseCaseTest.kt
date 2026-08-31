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
import com.quno.qunobackend.application.user.usecase.InMemoryUserRepository
import com.quno.qunobackend.domain.comment.CommentTargetType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ListCommentsUseCaseTest {
    private val questionRepository = InMemoryQuestionRepository()
    private val tagRepository = InMemoryTagRepository()
    private val createQuestionUseCase = CreateQuestionUseCase(
        questionRepository, InMemoryQuestionVersionRepository(), tagRepository, InMemoryQuestionTagRepository(tagRepository),
    )
    private val commentRepository = InMemoryCommentRepository()
    private val createCommentUseCase = CreateCommentUseCase(
        questionRepository, InMemoryAnswerRepository(), commentRepository, InMemoryUserRepository(), InMemoryOutboxEventRepository(),
    )
    private val deleteCommentUseCase = DeleteCommentUseCase(commentRepository)
    private val listCommentsUseCase = ListCommentsUseCase(commentRepository)

    private fun aQuestion(): Long = createQuestionUseCase.execute(
        CreateQuestionCommand(authorId = 1L, title = "t", body = "body", environment = null, logs = null),
    ).id

    @Test
    fun `lists comments oldest first`() {
        val questionId = aQuestion()
        createCommentUseCase.execute(CreateCommentCommand(CommentTargetType.QUESTION, questionId, authorId = 2L, body = "first"))
        createCommentUseCase.execute(CreateCommentCommand(CommentTargetType.QUESTION, questionId, authorId = 3L, body = "second"))

        val result = listCommentsUseCase.execute(CommentTargetType.QUESTION, questionId)

        assertEquals(listOf("first", "second"), result.map { it.body })
    }

    @Test
    fun `a deleted comment still appears in the list but with a null body`() {
        val questionId = aQuestion()
        val commentId = createCommentUseCase.execute(
            CreateCommentCommand(CommentTargetType.QUESTION, questionId, authorId = 2L, body = "oops"),
        ).id
        deleteCommentUseCase.execute(commentId, actorId = 2L)

        val result = listCommentsUseCase.execute(CommentTargetType.QUESTION, questionId)

        assertEquals(1, result.size)
        assertNull(result.single().body)
        assertEquals(true, result.single().isDeleted)
    }

    @Test
    fun `returns nothing for a target with no comments`() {
        val questionId = aQuestion()

        assertEquals(emptyList(), listCommentsUseCase.execute(CommentTargetType.QUESTION, questionId))
    }
}
