package com.quno.qunobackend.interfaces.api.save

import com.quno.qunobackend.domain.question.QuestionStatus

data class SavedQuestionResponse(
    val questionId: Long,
    val title: String,
    val status: QuestionStatus,
)
