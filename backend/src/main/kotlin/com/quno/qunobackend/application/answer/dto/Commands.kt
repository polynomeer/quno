package com.quno.qunobackend.application.answer.dto

data class WriteAnswerCommand(
    val questionId: Long,
    val authorId: Long,
    val body: String,
    /** Set only by WriteAnswerFromLiveChatUseCase after it has checked the room (ADR-0072). */
    val sourceLiveChatRoomId: Long? = null,
)

data class ReviseAnswerCommand(
    val answerId: Long,
    val actorId: Long,
    val body: String,
)

data class AcceptAnswerCommand(
    val answerId: Long,
    val actorId: Long,
)
