package com.quno.qunobackend.infrastructure.external

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.quno.qunobackend.domain.qunobot.FetchedTechnologyRelease
import com.quno.qunobackend.domain.qunobot.TechnologyReleaseFeed
import org.slf4j.LoggerFactory
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import java.time.LocalDate

/**
 * Talks to endoflife.date's public v1 API (no auth, no rate-limit key required) — the external
 * release-data source Phase 21/ADR-0033 introduces. `result.releases[0].latest` is that
 * product's single most recent release across all of its cycles: endoflife.date orders
 * `releases[]` newest-cycle-first, and each cycle carries its own `latest` patch release.
 */
@Component
class EndOfLifeDateTechnologyReleaseFeed : TechnologyReleaseFeed {

    private val logger = LoggerFactory.getLogger(EndOfLifeDateTechnologyReleaseFeed::class.java)

    private val restClient = RestClient.builder()
        .baseUrl("https://endoflife.date/api/v1/products")
        .requestFactory(
            SimpleClientHttpRequestFactory().apply {
                setConnectTimeout(5_000)
                setReadTimeout(5_000)
            },
        )
        .build()

    override fun fetchLatest(productSlug: String): FetchedTechnologyRelease? {
        val response = try {
            restClient.get().uri("/{productSlug}", productSlug).retrieve().body(EndOfLifeResponse::class.java)
        } catch (e: RestClientException) {
            logger.warn("qunobot.version-scan: fetch failed for productSlug={} ({})", productSlug, e.message)
            return null
        }

        val latest = response?.result?.releases?.firstOrNull()?.latest ?: return null
        val releaseDate = try {
            LocalDate.parse(latest.date)
        } catch (e: java.time.format.DateTimeParseException) {
            logger.warn("qunobot.version-scan: unparseable release date for productSlug={} ({})", productSlug, latest.date)
            return null
        }
        return FetchedTechnologyRelease(version = latest.name, releaseDate = releaseDate)
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class EndOfLifeResponse(val result: EndOfLifeResult?)

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class EndOfLifeResult(val releases: List<EndOfLifeReleaseCycle> = emptyList())

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class EndOfLifeReleaseCycle(val latest: EndOfLifeLatest?)

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class EndOfLifeLatest(val name: String, val date: String)
}
