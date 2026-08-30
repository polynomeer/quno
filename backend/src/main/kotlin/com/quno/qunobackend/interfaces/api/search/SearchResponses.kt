package com.quno.qunobackend.interfaces.api.search

import com.quno.qunobackend.application.search.dto.QuestionSearchResult
import com.quno.qunobackend.domain.question.QuestionStatus

data class QuestionSearchResultResponse(
    val id: Long,
    val title: String,
    val status: QuestionStatus,
    val tags: List<String>,
    val score: Long,
)

fun QuestionSearchResult.toResponse() =
    QuestionSearchResultResponse(id = id, title = title, status = status, tags = tags, score = score)
