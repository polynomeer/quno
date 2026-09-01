package com.quno.qunobackend.application.qunobot.usecase

import com.quno.qunobackend.domain.qunobot.FetchedTechnologyRelease
import com.quno.qunobackend.domain.qunobot.TechnologyReleaseFeed

class FakeTechnologyReleaseFeed : TechnologyReleaseFeed {
    /** Missing entries simulate "unknown to the feed / call failed" — [fetchLatest] returns null. */
    var releasesByProductSlug: Map<String, FetchedTechnologyRelease> = emptyMap()
    val fetchedProductSlugs = mutableListOf<String>()

    override fun fetchLatest(productSlug: String): FetchedTechnologyRelease? {
        fetchedProductSlugs += productSlug
        return releasesByProductSlug[productSlug]
    }
}
