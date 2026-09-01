package com.quno.qunobackend.application.livechat.usecase

import com.quno.qunobackend.application.livechat.dto.LiveChatRoomResult
import com.quno.qunobackend.domain.livechat.LiveChatRoomNotFoundException
import com.quno.qunobackend.domain.livechat.LiveChatRoomRepository
import org.springframework.stereotype.Service

@Service
class GetLiveChatRoomUseCase(
    private val liveChatRoomRepository: LiveChatRoomRepository,
) {
    fun execute(questionId: Long): LiveChatRoomResult =
        (liveChatRoomRepository.findByQuestionId(questionId) ?: throw LiveChatRoomNotFoundException(questionId)).toResult()
}
