package com.quno.qunobackend.interfaces.api.livechat

import com.quno.qunobackend.application.common.NicknameResolver
import com.quno.qunobackend.application.livechat.usecase.CreateAnswerDraftFromLiveChatUseCase
import com.quno.qunobackend.application.livechat.usecase.GetLiveChatRoomUseCase
import com.quno.qunobackend.application.livechat.usecase.ListLiveChatMessagesUseCase
import com.quno.qunobackend.application.livechat.usecase.OpenLiveChatRoomUseCase
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/** REST side of Live Chat (Phase 24, ADR-0036) — actual messaging happens over STOMP/WebSocket,
 * see infrastructure/websocket/LiveChatWebSocketController. This controller only opens/looks up
 * a room and serves the message history a client needs before its WebSocket subscription starts
 * delivering new ones. */
@RestController
class LiveChatController(
    private val openLiveChatRoomUseCase: OpenLiveChatRoomUseCase,
    private val getLiveChatRoomUseCase: GetLiveChatRoomUseCase,
    private val listLiveChatMessagesUseCase: ListLiveChatMessagesUseCase,
    private val createAnswerDraftFromLiveChatUseCase: CreateAnswerDraftFromLiveChatUseCase,
    private val nicknameResolver: NicknameResolver,
) {

    @PostMapping("/api/v1/questions/{questionId}/live-chat")
    @ResponseStatus(HttpStatus.CREATED)
    fun open(@AuthenticationPrincipal userId: Long, @PathVariable questionId: Long): LiveChatRoomResponse = openLiveChatRoomUseCase.execute(questionId, userId).toResponse()

    @GetMapping("/api/v1/questions/{questionId}/live-chat")
    fun get(@PathVariable questionId: Long): LiveChatRoomResponse = getLiveChatRoomUseCase.execute(questionId).toResponse()

    @GetMapping("/api/v1/live-chat/{roomId}/messages")
    fun messages(@PathVariable roomId: Long, @RequestParam(required = false) limit: Int?): List<LiveChatMessageResponse> {
        val messages = listLiveChatMessagesUseCase.execute(roomId, limit ?: 50)
        // Same one-batch nickname lookup as other list endpoints (ADR-0064); live STOMP frames
        // still carry only senderId.
        val nicknames = nicknameResolver.nicknamesOf(messages.map { it.senderId })
        return messages.map { it.toResponse(senderNickname = nicknames[it.senderId]) }
    }

    /** Unsaved markdown draft from selected messages — published later as a normal answer that
     * cites the room (Phase 71, ADR-0073). */
    @PostMapping("/api/v1/live-chat/{roomId}/answer-draft")
    fun answerDraft(
        @AuthenticationPrincipal userId: Long,
        @PathVariable roomId: Long,
        @Valid @RequestBody request: LiveChatAnswerDraftRequest,
    ): LiveChatAnswerDraftResponse = createAnswerDraftFromLiveChatUseCase.execute(roomId, userId, request.messageIds).toResponse()
}
