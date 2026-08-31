package com.quno.qunobackend.application.comment.usecase

import com.quno.qunobackend.application.answer.dto.WriteAnswerCommand
import com.quno.qunobackend.application.answer.usecase.InMemoryAnswerRepository
import com.quno.qunobackend.application.answer.usecase.InMemoryAnswerVersionRepository
import com.quno.qunobackend.application.answer.usecase.WriteAnswerUseCase
import com.quno.qunobackend.application.comment.dto.CreateCommentCommand
import com.quno.qunobackend.application.common.AnswerResultAssembler
import com.quno.qunobackend.application.common.InMemoryOutboxEventRepository
import com.quno.qunobackend.application.question.dto.CreateQuestionCommand
import com.quno.qunobackend.application.question.usecase.CreateQuestionUseCase
import com.quno.qunobackend.application.question.usecase.InMemoryQuestionRepository
import com.quno.qunobackend.application.question.usecase.InMemoryQuestionVersionRepository
import com.quno.qunobackend.application.tag.usecase.InMemoryQuestionTagRepository
import com.quno.qunobackend.application.tag.usecase.InMemoryTagRepository
import com.quno.qunobackend.application.user.usecase.InMemoryUserRepository
import com.quno.qunobackend.application.vote.usecase.InMemoryVoteRepository
import com.quno.qunobackend.domain.answer.AnswerNotFoundException
import com.quno.qunobackend.domain.comment.CommentNotFoundException
import com.quno.qunobackend.domain.comment.CommentReplyDepthExceededException
import com.quno.qunobackend.domain.comment.CommentTargetType
import com.quno.qunobackend.domain.common.OutboxEventTypes
import com.quno.qunobackend.domain.question.QuestionNotFoundException
import com.quno.qunobackend.domain.user.User
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CreateCommentUseCaseTest {
    private val questionRepository = InMemoryQuestionRepository()
    private val questionVersionRepository = InMemoryQuestionVersionRepository()
    private val tagRepository = InMemoryTagRepository()
    private val answerRepository = InMemoryAnswerRepository()
    private val commentRepository = InMemoryCommentRepository()
    private val userRepository = InMemoryUserRepository()
    private val outboxEventRepository = InMemoryOutboxEventRepository()

    private val createQuestionUseCase = CreateQuestionUseCase(
        questionRepository, questionVersionRepository, tagRepository, InMemoryQuestionTagRepository(tagRepository),
    )
    private val writeAnswerUseCase = WriteAnswerUseCase(
        questionRepository, questionVersionRepository, answerRepository, InMemoryAnswerVersionRepository(), outboxEventRepository,
        AnswerResultAssembler(questionRepository, questionVersionRepository, InMemoryVoteRepository()),
    )
    private val useCase = CreateCommentUseCase(questionRepository, answerRepository, commentRepository, userRepository, outboxEventRepository)

    private fun questionAskedBy(authorId: Long): Long = createQuestionUseCase.execute(
        CreateQuestionCommand(authorId = authorId, title = "t", body = "body", environment = null, logs = null),
    ).id

    private fun userNamed(nickname: String): Long =
        requireNotNull(userRepository.save(User.register("$nickname@example.com", nickname, "hash")).id)

    @Test
    fun `comments on a question`() {
        val questionId = questionAskedBy(authorId = 1L)

        val result = useCase.execute(CreateCommentCommand(CommentTargetType.QUESTION, questionId, authorId = 2L, body = "can you share logs?"))

        assertEquals("can you share logs?", result.body)
        assertEquals(false, result.isDeleted)
        assertEquals(null, result.parentCommentId)
        assertEquals(1, result.versionNumber)
    }

    @Test
    fun `comments on an answer and records the question's id as the outbox aggregate`() {
        val questionId = questionAskedBy(authorId = 1L)
        val answerId = writeAnswerUseCase.execute(WriteAnswerCommand(questionId, authorId = 2L, body = "answer")).id

        useCase.execute(CreateCommentCommand(CommentTargetType.ANSWER, answerId, authorId = 3L, body = "nice answer"))

        val event = outboxEventRepository.events.single { it.eventType == OutboxEventTypes.NEW_COMMENT }
        assertEquals(questionId, event.aggregateId)
        assertTrue(event.payload.contains("\"questionAuthorId\":1"))
        assertTrue(event.payload.contains("\"answerAuthorId\":2"))
    }

    @Test
    fun `a question comment's payload has no answerAuthorId`() {
        val questionId = questionAskedBy(authorId = 1L)

        useCase.execute(CreateCommentCommand(CommentTargetType.QUESTION, questionId, authorId = 2L, body = "hi"))

        val event = outboxEventRepository.events.single { it.eventType == OutboxEventTypes.NEW_COMMENT }
        assertTrue(event.payload.contains("\"answerAuthorId\":null"))
    }

    @Test
    fun `rejects a blank body`() {
        val questionId = questionAskedBy(authorId = 1L)

        assertFailsWith<IllegalArgumentException> {
            useCase.execute(CreateCommentCommand(CommentTargetType.QUESTION, questionId, authorId = 2L, body = "   "))
        }
    }

    @Test
    fun `rejects a body over 600 characters`() {
        val questionId = questionAskedBy(authorId = 1L)

        assertFailsWith<IllegalArgumentException> {
            useCase.execute(CreateCommentCommand(CommentTargetType.QUESTION, questionId, authorId = 2L, body = "a".repeat(601)))
        }
    }

    @Test
    fun `rejects commenting on a question that does not exist`() {
        assertFailsWith<QuestionNotFoundException> {
            useCase.execute(CreateCommentCommand(CommentTargetType.QUESTION, 999L, authorId = 2L, body = "hi"))
        }
    }

    @Test
    fun `rejects commenting on an answer that does not exist`() {
        assertFailsWith<AnswerNotFoundException> {
            useCase.execute(CreateCommentCommand(CommentTargetType.ANSWER, 999L, authorId = 2L, body = "hi"))
        }
    }

    @Test
    fun `rejects replying to a parent comment that does not exist`() {
        val questionId = questionAskedBy(authorId = 1L)

        assertFailsWith<CommentNotFoundException> {
            useCase.execute(CreateCommentCommand(CommentTargetType.QUESTION, questionId, authorId = 2L, body = "hi", parentCommentId = 999L))
        }
    }

    @Test
    fun `a reply carries the parent's id and notifies the parent's author`() {
        val questionId = questionAskedBy(authorId = 1L)
        val parentId = useCase.execute(
            CreateCommentCommand(CommentTargetType.QUESTION, questionId, authorId = 2L, body = "parent"),
        ).id

        val reply = useCase.execute(
            CreateCommentCommand(CommentTargetType.QUESTION, questionId, authorId = 3L, body = "reply", parentCommentId = parentId),
        )

        assertEquals(parentId, reply.parentCommentId)
        val replyEvent = outboxEventRepository.events.filter { it.eventType == OutboxEventTypes.NEW_COMMENT }.last()
        assertTrue(replyEvent.payload.contains("\"parentCommentAuthorId\":2"))
    }

    @Test
    fun `rejects replying to a reply (one level of nesting only)`() {
        val questionId = questionAskedBy(authorId = 1L)
        val parentId = useCase.execute(
            CreateCommentCommand(CommentTargetType.QUESTION, questionId, authorId = 2L, body = "parent"),
        ).id
        val replyId = useCase.execute(
            CreateCommentCommand(CommentTargetType.QUESTION, questionId, authorId = 3L, body = "reply", parentCommentId = parentId),
        ).id

        assertFailsWith<CommentReplyDepthExceededException> {
            useCase.execute(
                CreateCommentCommand(CommentTargetType.QUESTION, questionId, authorId = 4L, body = "reply to reply", parentCommentId = replyId),
            )
        }
    }

    @Test
    fun `mentioning a real user's nickname notifies them`() {
        val questionId = questionAskedBy(authorId = 1L)
        val bobId = userNamed("bob")

        useCase.execute(CreateCommentCommand(CommentTargetType.QUESTION, questionId, authorId = 2L, body = "hey @bob check this out"))

        val event = outboxEventRepository.events.single { it.eventType == OutboxEventTypes.MENTIONED_IN_COMMENT }
        assertTrue(event.payload.contains("\"mentionedUserIds\":[$bobId]"))
    }

    @Test
    fun `mentioning yourself does not notify you`() {
        val questionId = questionAskedBy(authorId = 1L)
        val aliceId = userNamed("alice")

        useCase.execute(CreateCommentCommand(CommentTargetType.QUESTION, questionId, authorId = aliceId, body = "note to self @alice"))

        assertTrue(outboxEventRepository.events.none { it.eventType == OutboxEventTypes.MENTIONED_IN_COMMENT })
    }

    @Test
    fun `mentioning a nickname that does not exist emits no mention event`() {
        val questionId = questionAskedBy(authorId = 1L)

        useCase.execute(CreateCommentCommand(CommentTargetType.QUESTION, questionId, authorId = 2L, body = "hey @nobody-here"))

        assertTrue(outboxEventRepository.events.none { it.eventType == OutboxEventTypes.MENTIONED_IN_COMMENT })
    }
}
