package com.quno.qunobackend.domain.livechat

/** [id] is a roomId or questionId depending on the caller — GetLiveChatRoomUseCase looks up by
 * question, PostLiveChatMessageUseCase by room; both "not found" cases collapse to the same 404. */
class LiveChatRoomNotFoundException(id: Long) : RuntimeException("Live chat room not found: $id")
