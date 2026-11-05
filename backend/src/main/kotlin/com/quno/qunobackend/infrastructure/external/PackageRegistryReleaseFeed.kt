package com.quno.qunobackend.infrastructure.external

import com.quno.qunobackend.domain.reproduction.PackageReleaseFeed
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import java.io.IOException
import java.net.URI
import java.net.URLEncoder

/** Where release lists come from (Phase 83, ADR-0091). */
@ConfigurationProperties(prefix = "quno.runner.registry")
data class PackageRegistryProperties(
    val mavenUrl: String = "https://repo1.maven.org/maven2",
    val npmUrl: String = "https://registry.npmjs.org",
)

@Configuration
@EnableConfigurationProperties(PackageRegistryProperties::class)
class PackageRegistryConfig

/**
 * Reads a package's versions from Maven Central (`group:artifact`) or npm (anything else). The name
 * comes from a question's author, so it must look like a package before it goes in a URL, and the
 * hosts are fixed. A failure returns null rather than throwing: the caller says "couldn't read the
 * list" and nothing else depends on a registry being up.
 */
@Component
class PackageRegistryReleaseFeed(
    private val properties: PackageRegistryProperties,
    private val objectMapper: ObjectMapper,
) : PackageReleaseFeed {
    private val logger = LoggerFactory.getLogger(PackageRegistryReleaseFeed::class.java)

    override fun fetchVersions(packageName: String): List<String>? = try {
        when {
            MAVEN.matches(packageName) && ".." !in packageName -> maven(packageName)
            NPM.matches(packageName) && ".." !in packageName -> npm(packageName)
            else -> null
        }
    } catch (e: IOException) {
        logger.warn("reproduction.bisect: release list failed for {} ({})", packageName, e.message)
        null
    } catch (e: tools.jackson.core.JacksonException) {
        logger.warn("reproduction.bisect: unreadable release list for {} ({})", packageName, e.message)
        null
    }

    private fun maven(packageName: String): List<String> {
        val (group, artifact) = packageName.split(':')
        val xml = read("${properties.mavenUrl}/${group.replace('.', '/')}/$artifact/maven-metadata.xml", null)
        return VERSION_TAG.findAll(xml).map { it.groupValues[1].trim() }.toList()
    }

    /** The abbreviated document: the full one carries every version's README. */
    private fun npm(packageName: String): List<String> {
        val encoded = URLEncoder.encode(packageName, Charsets.UTF_8).replace("%40", "@")
        val json = read("${properties.npmUrl}/$encoded", "application/vnd.npm.install-v1+json")
        return objectMapper.readTree(json).path("versions").propertyNames().toList()
    }

    private fun read(url: String, accept: String?): String {
        val connection = URI.create(url).toURL().openConnection().apply {
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            accept?.let { setRequestProperty("Accept", it) }
        }
        return connection.getInputStream().use { stream ->
            val bytes = stream.readNBytes(MAX_BYTES + 1)
            if (bytes.size > MAX_BYTES) throw IOException("release list over $MAX_BYTES bytes")
            String(bytes, Charsets.UTF_8)
        }
    }

    companion object {
        private const val TIMEOUT_MS = 5_000
        private const val MAX_BYTES = 8 * 1024 * 1024
        private val MAVEN = Regex("""[\w.\-]+:[\w.\-]+""")
        private val NPM = Regex("""(@[\w.\-]+/)?[\w.\-]+""")
        private val VERSION_TAG = Regex("""<version>([^<]+)</version>""")
    }
}
