package com.quno.qunobackend.interfaces.api.search

import com.quno.qunobackend.application.search.usecase.QuestionSearchUseCase
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/search")
class SearchController(
    private val questionSearchUseCase: QuestionSearchUseCase,
) {

    @GetMapping
    fun search(
        @RequestParam q: String,
        @RequestParam(required = false) limit: Int?,
    ): List<QuestionSearchResultResponse> =
        questionSearchUseCase.search(q, limit ?: 20).map { it.toResponse() }
}
