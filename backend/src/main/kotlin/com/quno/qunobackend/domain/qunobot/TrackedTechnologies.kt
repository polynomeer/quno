package com.quno.qunobackend.domain.qunobot

import com.quno.qunobackend.domain.stack.Ecosystem
import com.quno.qunobackend.domain.stack.PackagePattern

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

    /**
     * Tag slug → the dependency coordinates whose version is that product's version (Phase 63.4,
     * ADR-0077), so answer applicability ranges can be judged against a new release. Server products
     * (redis, postgresql, …) have no coordinate that carries the server's version and stay unmapped.
     */
    val PACKAGES: Map<String, List<PackagePattern>> = mapOf(
        "kotlin" to listOf(
            PackagePattern(Ecosystem.GRADLE_PLUGIN, "org.jetbrains.kotlin.*"),
            PackagePattern(Ecosystem.MAVEN, "org.jetbrains.kotlin:kotlin-*"),
        ),
        "spring-boot" to listOf(
            PackagePattern(Ecosystem.GRADLE_PLUGIN, "org.springframework.boot"),
            PackagePattern(Ecosystem.MAVEN, "org.springframework.boot:spring-boot*"),
        ),
        "kafka" to listOf(PackagePattern(Ecosystem.MAVEN, "org.apache.kafka:kafka-clients")),
    )
}
