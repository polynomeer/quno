package com.quno.qunobackend.application.save.dto

import com.quno.qunobackend.domain.question.QuestionStatus

data class SavedQuestionResult(
    val questionId: Long,
    val title: String,
    val status: QuestionStatus,
)
