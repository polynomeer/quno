package com.quno.qunobackend.application.search.dto

import com.quno.qunobackend.domain.question.QuestionStatus

data class QuestionSearchResult(
    val id: Long,
    val title: String,
    val status: QuestionStatus,
    val tags: List<String>,
)
