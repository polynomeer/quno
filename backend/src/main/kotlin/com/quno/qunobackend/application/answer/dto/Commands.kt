package com.quno.qunobackend.application.answer.dto

data class WriteAnswerCommand(
    val questionId: Long,
    val authorId: Long,
    val body: String,
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
