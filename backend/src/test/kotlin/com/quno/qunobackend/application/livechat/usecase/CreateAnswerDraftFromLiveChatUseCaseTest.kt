package com.quno.qunobackend.application.livechat.usecase

import com.quno.qunobackend.application.common.NicknameResolver
import com.quno.qunobackend.application.livechat.LiveChatParticipantPolicy
import com.quno.qunobackend.application.user.usecase.InMemoryUserRepository
import com.quno.qunobackend.domain.livechat.InvalidLiveChatMessageSelectionException
import com.quno.qunobackend.domain.livechat.LiveChatMessage
import com.quno.qunobackend.domain.livechat.LiveChatParticipantRequiredException
import com.quno.qunobackend.domain.livechat.LiveChatRoom
import com.quno.qunobackend.domain.livechat.LiveChatRoomNotFoundException
import com.quno.qunobackend.domain.user.User
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CreateAnswerDraftFromLiveChatUseCaseTest {
    private val roomRepository = InMemoryLiveChatRoomRepository()
    private val messageRepository = InMemoryLiveChatMessageRepository()
    private val userRepository = InMemoryUserRepository()
    private val useCase = CreateAnswerDraftFromLiveChatUseCase(
        LiveChatParticipantPolicy(roomRepository, messageRepository),
        messageRepository,
        NicknameResolver(userRepository),
    )

    private val opener = requireNotNull(userRepository.save(User.register("a@example.com", "minji", "hash")).id)
    private val helper = requireNotNull(userRepository.save(User.register("b@example.com", "devpark", "hash")).id)
    private val outsider = requireNotNull(userRepository.save(User.register("c@example.com", "outsider", "hash")).id)
    private val roomId = requireNotNull(roomRepository.save(LiveChatRoom.open(questionId = 10L, createdBy = opener)).id)

    private fun message(id: String, senderId: Long, body: String, at: Long, room: Long = roomId): String {
        messageRepository.save(LiveChatMessage.reconstitute(id = id, roomId = room, senderId = senderId, body = body, createdAt = Instant.ofEpochSecond(at)))
        return id
    }

    @Test
    fun `quotes the selected messages in time order with each sender's nickname`() {
        val later = message("m2", helper, "auditScope로 분리하세요.\n코드는 아래와 같아요.", at = 200)
        val earlier = message("m1", opener, "풀이 고갈돼요.", at = 100)

        val draft = useCase.execute(roomId, userId = helper, messageIds = listOf(later, earlier))

        assertEquals(10L, draft.questionId)
        assertEquals(2, draft.messageCount)
        assertTrue(draft.body.indexOf("**minji**\n> 풀이 고갈돼요.") < draft.body.indexOf("**devpark**"))
        assertTrue(draft.body.contains("> auditScope로 분리하세요.  \n> 코드는 아래와 같아요."))
    }

    @Test
    fun `the room opener may draft even before posting anything`() {
        val id = message("m1", helper, "hi", at = 100)

        assertEquals(1, useCase.execute(roomId, userId = opener, messageIds = listOf(id)).messageCount)
    }

    @Test
    fun `someone who never took part in the room is refused`() {
        val id = message("m1", helper, "hi", at = 100)

        assertFailsWith<LiveChatParticipantRequiredException> { useCase.execute(roomId, userId = outsider, messageIds = listOf(id)) }
    }

    @Test
    fun `messages from another room or unknown ids are rejected`() {
        val otherRoom = requireNotNull(roomRepository.save(LiveChatRoom.open(questionId = 11L, createdBy = opener)).id)
        val own = message("m1", helper, "hi", at = 100)
        val foreign = message("x1", helper, "elsewhere", at = 100, room = otherRoom)

        assertFailsWith<InvalidLiveChatMessageSelectionException> { useCase.execute(roomId, helper, listOf(own, foreign)) }
        assertFailsWith<InvalidLiveChatMessageSelectionException> { useCase.execute(roomId, helper, listOf(own, "missing")) }
    }

    @Test
    fun `an empty or oversized selection is rejected`() {
        message("m1", helper, "hi", at = 100)

        assertFailsWith<InvalidLiveChatMessageSelectionException> { useCase.execute(roomId, helper, emptyList()) }
        assertFailsWith<InvalidLiveChatMessageSelectionException> { useCase.execute(roomId, helper, (1..51).map { "id$it" }) }
    }

    @Test
    fun `a room that does not exist is a 404`() {
        assertFailsWith<LiveChatRoomNotFoundException> { useCase.execute(999L, helper, listOf("m1")) }
    }
}
