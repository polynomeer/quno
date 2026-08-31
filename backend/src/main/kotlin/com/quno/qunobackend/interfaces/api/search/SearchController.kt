package com.quno.qunobackend.interfaces.api.search

import com.quno.qunobackend.application.search.usecase.QuestionSearchUseCase
import com.quno.qunobackend.domain.search.SearchSort
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
        /** Unrecognized/missing values fall back to relevance (Phase 20, ADR-0032) — a sort
         * toggle isn't worth a validation error over. */
        @RequestParam(required = false) sort: String?,
    ): List<QuestionSearchResultResponse> {
        val resolvedSort = SearchSort.entries.find { it.name.equals(sort, ignoreCase = true) } ?: SearchSort.RELEVANCE
        return questionSearchUseCase.search(q, limit ?: 20, resolvedSort).map { it.toResponse() }
    }
}
