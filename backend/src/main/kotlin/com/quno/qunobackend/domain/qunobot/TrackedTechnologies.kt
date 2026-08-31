package com.quno.qunobackend.domain.qunobot

/**
 * Tag slug → endoflife.date product slug, curated by hand (Phase 21, ADR-0033) — same
 * "explicit over auto-discovered" philosophy as Cluster (ADR-0016) and Outdated (ADR-0017).
 * Limited to technologies with an unambiguous single product on endoflife.date; e.g. "java" was
 * deliberately left out because endoflife.date only lists vendor-specific JDK builds
 * (amazon-corretto, eclipse-temurin, ...), and picking one would misrepresent "java" questions
 * in general. Extend this map as real tag usage calls for it — nothing else needs to change.
 */
object TrackedTechnologies {
    val MAPPING: Map<String, String> = mapOf(
        "kotlin" to "kotlin",
        "spring-boot" to "spring-boot",
        "redis" to "redis",
        "kafka" to "apache-kafka",
        "postgresql" to "postgresql",
        "mongodb" to "mongodb",
        "docker" to "docker-engine",
    )
}
