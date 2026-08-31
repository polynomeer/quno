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
import com.quno.qunobackend.domain.comment.CommentNotFoundException
import com.quno.qunobackend.domain.comment.CommentTargetType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ListCommentVersionsUseCaseTest {
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
    private val editCommentUseCase = EditCommentUseCase(commentRepository, commentVersionRepository)
    private val useCase = ListCommentVersionsUseCase(commentRepository, commentVersionRepository)

    @Test
    fun `an unedited comment has no version history`() {
        val questionId = createQuestionUseCase.execute(
            CreateQuestionCommand(authorId = 1L, title = "t", body = "body", environment = null, logs = null),
        ).id
        val commentId = createCommentUseCase.execute(
            CreateCommentCommand(CommentTargetType.QUESTION, questionId, authorId = 2L, body = "original"),
        ).id

        assertEquals(emptyList(), useCase.execute(commentId))
    }

    @Test
    fun `an edited comment lists its prior bodies oldest first`() {
        val questionId = createQuestionUseCase.execute(
            CreateQuestionCommand(authorId = 1L, title = "t", body = "body", environment = null, logs = null),
        ).id
        val commentId = createCommentUseCase.execute(
            CreateCommentCommand(CommentTargetType.QUESTION, questionId, authorId = 2L, body = "v1"),
        ).id
        editCommentUseCase.execute(EditCommentCommand(commentId, editorId = 2L, body = "v2"))
        editCommentUseCase.execute(EditCommentCommand(commentId, editorId = 2L, body = "v3"))

        val versions = useCase.execute(commentId)

        assertEquals(listOf(1 to "v1", 2 to "v2"), versions.map { it.versionNumber to it.body })
    }

    @Test
    fun `rejects listing versions for a comment that does not exist`() {
        assertFailsWith<CommentNotFoundException> {
            useCase.execute(999L)
        }
    }
}
