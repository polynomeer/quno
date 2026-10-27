package com.quno.qunobackend.application.answer.usecase

import com.quno.qunobackend.application.answer.dto.WriteAnswerCommand
import com.quno.qunobackend.application.common.AnswerResultAssembler
import com.quno.qunobackend.application.common.InMemoryOutboxEventRepository
import com.quno.qunobackend.application.livechat.LiveChatParticipantPolicy
import com.quno.qunobackend.application.livechat.usecase.InMemoryLiveChatMessageRepository
import com.quno.qunobackend.application.livechat.usecase.InMemoryLiveChatRoomRepository
import com.quno.qunobackend.application.question.dto.CreateQuestionCommand
import com.quno.qunobackend.application.question.usecase.CreateQuestionUseCase
import com.quno.qunobackend.application.question.usecase.InMemoryQuestionRepository
import com.quno.qunobackend.application.question.usecase.InMemoryQuestionVersionRepository
import com.quno.qunobackend.application.tag.usecase.InMemoryQuestionTagRepository
import com.quno.qunobackend.application.tag.usecase.InMemoryTagRepository
import com.quno.qunobackend.application.vote.usecase.InMemoryVoteRepository
import com.quno.qunobackend.domain.common.OutboxEventTypes
import com.quno.qunobackend.domain.livechat.LiveChatMessage
import com.quno.qunobackend.domain.livechat.LiveChatParticipantRequiredException
import com.quno.qunobackend.domain.livechat.LiveChatRoom
import com.quno.qunobackend.domain.livechat.LiveChatRoomQuestionMismatchException
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class WriteAnswerFromLiveChatUseCaseTest {
    private val questionRepository = InMemoryQuestionRepository()
    private val questionVersionRepository = InMemoryQuestionVersionRepository()
    private val answerRepository = InMemoryAnswerRepository()
    private val tagRepository = InMemoryTagRepository()
    private val outboxEventRepository = InMemoryOutboxEventRepository()
    private val roomRepository = InMemoryLiveChatRoomRepository()
    private val messageRepository = InMemoryLiveChatMessageRepository()
    private val createQuestionUseCase = CreateQuestionUseCase(
        questionRepository,
        questionVersionRepository,
        tagRepository,
        InMemoryQuestionTagRepository(tagRepository),
    )
    private val writeAnswerUseCase = WriteAnswerUseCase(
        questionRepository,
        questionVersionRepository,
        answerRepository,
        InMemoryAnswerVersionRepository(),
        outboxEventRepository,
        AnswerResultAssembler(questionRepository, questionVersionRepository, InMemoryVoteRepository()),
    )
    private val useCase = WriteAnswerFromLiveChatUseCase(LiveChatParticipantPolicy(roomRepository, messageRepository), writeAnswerUseCase)

    private fun question(): Long = createQuestionUseCase.execute(
        CreateQuestionCommand(authorId = 1L, title = "t", body = "body", environment = null, logs = null),
    ).id

    @Test
    fun `publishes a normal answer that cites the room`() {
        val questionId = question()
        val roomId = requireNotNull(roomRepository.save(LiveChatRoom.open(questionId, createdBy = 1L)).id)
        messageRepository.save(LiveChatMessage.post(roomId, senderId = 2L, body = "try this"))

        val result = useCase.execute(WriteAnswerCommand(questionId, authorId = 2L, body = "정리한 답변"), roomId)

        assertEquals(roomId, result.sourceLiveChatRoomId)
        assertEquals(roomId, answerRepository.findById(result.id)?.sourceLiveChatRoomId)
        assertEquals(OutboxEventTypes.NEW_ANSWER, outboxEventRepository.findUnpublished(10).single().eventType)
    }

    @Test
    fun `an ordinary answer has no source room`() {
        val result = writeAnswerUseCase.execute(WriteAnswerCommand(question(), authorId = 2L, body = "plain"))

        assertNull(result.sourceLiveChatRoomId)
    }

    @Test
    fun `a room from another question cannot be cited`() {
        val questionId = question()
        val otherRoom = requireNotNull(roomRepository.save(LiveChatRoom.open(question(), createdBy = 2L)).id)

        assertFailsWith<LiveChatRoomQuestionMismatchException> {
            useCase.execute(WriteAnswerCommand(questionId, authorId = 2L, body = "x"), otherRoom)
        }
    }

    @Test
    fun `only room participants can cite it`() {
        val questionId = question()
        val roomId = requireNotNull(roomRepository.save(LiveChatRoom.open(questionId, createdBy = 1L)).id)

        assertFailsWith<LiveChatParticipantRequiredException> {
            useCase.execute(WriteAnswerCommand(questionId, authorId = 3L, body = "x"), roomId)
        }
    }
}
