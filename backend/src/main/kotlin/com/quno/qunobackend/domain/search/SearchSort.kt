package com.quno.qunobackend.domain.search

/** `relevance` is the pre-existing default — actually `id DESC` (recency), not a true full-text
 * ranking, since the search query never computed `ts_rank` (see SearchRepository kdoc). `score`
 * orders by the question's net vote score instead (Phase 20, ADR-0032). */
enum class SearchSort {
    RELEVANCE,
    SCORE,
}
