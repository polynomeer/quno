package com.quno.qunobackend.application.answer.dto

import com.quno.qunobackend.domain.stack.ApplicabilityRange

data class WriteAnswerCommand(
    val questionId: Long,
    val authorId: Long,
    val body: String,
    /** Set only by WriteAnswerFromLiveChatUseCase after it has checked the room (ADR-0073). */
    val sourceLiveChatRoomId: Long? = null,
    /** Where the answer holds, per package (Phase 63.3, ADR-0077). */
    val applicability: List<ApplicabilityRange> = emptyList(),
)

data class ReviseAnswerCommand(
    val answerId: Long,
    val actorId: Long,
    val body: String,
    /** null keeps the current ranges, an empty list clears them (ADR-0077). */
    val applicability: List<ApplicabilityRange>? = null,
)

data class AcceptAnswerCommand(
    val answerId: Long,
    val actorId: Long,
)
