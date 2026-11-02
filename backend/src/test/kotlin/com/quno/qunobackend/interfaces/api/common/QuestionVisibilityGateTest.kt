package com.quno.qunobackend.interfaces.api.common

import com.quno.qunobackend.application.common.QuestionScopedResource
import com.quno.qunobackend.infrastructure.websocket.StompQuestionVisibilityInterceptor
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.test.assertEquals
import kotlin.test.assertNull

class QuestionVisibilityGateTest {
    @ParameterizedTest
    @CsvSource(
        "/api/v1/questions/12, QUESTION, 12",
        "/api/v1/questions/12/versions/3/diff, QUESTION, 12",
        "/api/v1/answers/7/comments, ANSWER, 7",
        "/api/v1/comments/5/versions, COMMENT, 5",
        "/api/v1/live-chat/9/messages, LIVE_CHAT_ROOM, 9",
        "/api/v1/direct-asks/4/accept, DIRECT_ASK, 4",
        "/api/v1/engine-suggestions/8/dismiss, ENGINE_SUGGESTION, 8",
    )
    fun `addresses under a question resolve to their resource`(path: String, resource: QuestionScopedResource, id: Long) {
        assertEquals(resource to id, QuestionVisibilityGate.match(path))
    }

    @ParameterizedTest
    @CsvSource("/api/v1/questions", "/api/v1/search", "/api/v1/questionsx/1", "/api/v1/me/saves", "/api/v1/clusters/3")
    fun `everything else passes through to its own checks`(path: String) {
        assertNull(QuestionVisibilityGate.match(path))
    }

    @ParameterizedTest
    @CsvSource(
        "/topic/questions/12/presence, QUESTION, 12",
        "/topic/live-chat/9, LIVE_CHAT_ROOM, 9",
        "/app/live-chat/9/send, LIVE_CHAT_ROOM, 9",
    )
    fun `stomp destinations resolve the same way`(destination: String, resource: QuestionScopedResource, id: Long) {
        assertEquals(resource to id, StompQuestionVisibilityInterceptor.match(destination))
    }
}
