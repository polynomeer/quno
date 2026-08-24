package com.quno.qunobackend.interfaces.api.watch

import com.quno.qunobackend.domain.question.QuestionStatus

data class WatchedQuestionResponse(
    val questionId: Long,
    val title: String,
    val status: QuestionStatus,
)
