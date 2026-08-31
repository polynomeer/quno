package com.quno.qunobackend.application.question.dto

data class CreateQuestionCommand(
    val authorId: Long,
    val title: String,
    val body: String,
    val environment: String?,
    val logs: String?,
    val tagNames: List<String> = emptyList(),
)

data class ReviseQuestionCommand(
    val questionId: Long,
    val actorId: Long,
    val title: String,
    val body: String,
    val environment: String?,
    val logs: String?,
)

data class MarkQuestionOutdatedCommand(
    val questionId: Long,
    val actorId: Long,
    val reason: String,
)

data class ForkQuestionCommand(
    val originQuestionId: Long,
    val actorId: Long,
)
