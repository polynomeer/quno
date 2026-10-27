package com.quno.qunobackend.domain.livechat

/** [id] is a roomId or questionId depending on the caller — GetLiveChatRoomUseCase looks up by
 * question, PostLiveChatMessageUseCase by room; both "not found" cases collapse to the same 404. */
class LiveChatRoomNotFoundException(id: Long) : RuntimeException("Live chat room not found: $id")

/** Only the person who opened the room or someone who has posted in it may turn the conversation
 * into an answer (Phase 71, ADR-0073). */
class LiveChatParticipantRequiredException(roomId: Long, userId: Long) : RuntimeException("User $userId is not a participant of live chat room $roomId")

/** An answer can only cite the live chat room of the question it answers. */
class LiveChatRoomQuestionMismatchException(roomId: Long, questionId: Long) : RuntimeException("Live chat room $roomId does not belong to question $questionId")

/** Some selected message ids are not in the room (or don't exist), or the selection is empty/too large. */
class InvalidLiveChatMessageSelectionException(message: String) : RuntimeException(message)
