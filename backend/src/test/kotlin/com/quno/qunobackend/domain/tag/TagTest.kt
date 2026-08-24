package com.quno.qunobackend.domain.tag

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class TagTest {

    @Test
    fun `create derives a lowercase, hyphenated slug`() {
        val tag = Tag.create("Spring Boot")

        assertEquals("Spring Boot", tag.name)
        assertEquals("spring-boot", tag.slug)
    }

    @Test
    fun `create rejects a blank name`() {
        assertFailsWith<IllegalArgumentException> { Tag.create(" ") }
    }

    @Test
    fun `rename recomputes the slug`() {
        val tag = Tag.create("Redis").rename("Redis Cluster")

        assertEquals("redis-cluster", tag.slug)
    }

    @Test
    fun `softDelete stamps deletedAt`() {
        val tag = Tag.create("Kafka")

        assertNotNull(tag.softDelete().deletedAt)
    }
}
