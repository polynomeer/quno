package com.quno.qunobackend.application.watch.dto

import com.quno.qunobackend.domain.question.QuestionStatus

data class WatchedQuestionResult(
    val questionId: Long,
    val title: String,
    val status: QuestionStatus,
)
