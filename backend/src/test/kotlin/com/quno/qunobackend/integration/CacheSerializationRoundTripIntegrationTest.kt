package com.quno.qunobackend.integration

import com.quno.qunobackend.domain.dashboard.TagTrend
import com.quno.qunobackend.domain.qunobot.TagSpike
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cache.CacheManager
import kotlin.test.assertEquals

/**
 * Writes a value into each Redis-backed `@Cacheable` cache and reads it back through the real
 * serializer (ADR-0065). A cache hit has to return the same Kotlin types the adapter produced on a
 * miss — before the fix, the untyped serializer handed back `LinkedHashMap`s (and `Integer`s for
 * small `Long`s), which only blew up later as a `ClassCastException` in the dashboard, and only on
 * the second request within the TTL.
 */
@SpringBootTest
class CacheSerializationRoundTripIntegrationTest {
    @Autowired
    lateinit var cacheManager: CacheManager

    private val key = "roundtrip-test-${System.nanoTime()}"

    @AfterEach
    fun evict() {
        listOf("dashboard-popular-questions", "dashboard-trending-tags", "qunobot-tag-spikes").forEach {
            cacheManager.getCache(it)?.evict(key)
        }
    }

    /** The Redis write can land after an immediate read (seen with MONITOR: GET reached Redis
     * before SET), so poll briefly instead of reading once. */
    private fun roundTrip(cacheName: String, value: Any): Any? {
        val cache = requireNotNull(cacheManager.getCache(cacheName)) { "no cache named $cacheName" }
        cache.put(key, value)
        repeat(50) {
            cache.get(key)?.let { return it.get() }
            Thread.sleep(20)
        }
        error("value never appeared in cache $cacheName")
    }

    @Test
    fun `trending tags come back as TagTrend`() {
        val original = listOf(TagTrend(id = 1L, name = "kotlin", slug = "kotlin", questionCount = 12L))

        val restored = roundTrip("dashboard-trending-tags", original) as List<*>

        assertEquals(TagTrend::class.java, restored.single()!!::class.java)
        assertEquals(original, restored)
    }

    @Test
    fun `tag spikes come back as TagSpike`() {
        val original = listOf(TagSpike(id = 2L, name = "r2dbc", slug = "r2dbc", recentCount = 20L, baselineAveragePerDay = 5.0, spikeRatio = 4.0))

        val restored = roundTrip("qunobot-tag-spikes", original) as List<*>

        assertEquals(TagSpike::class.java, restored.single()!!::class.java)
        assertEquals(original, restored)
    }

    @Test
    fun `popular question ids come back as Long even when small`() {
        val original = listOf(7L, 66L)

        val restored = roundTrip("dashboard-popular-questions", original) as List<*>

        assertEquals(listOf(java.lang.Long::class.java, java.lang.Long::class.java), restored.map { it!!::class.javaObjectType })
        assertEquals(original, restored)
    }
}
