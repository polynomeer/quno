package com.quno.qunobackend.application.tag.usecase

import com.quno.qunobackend.domain.tag.Tag
import com.quno.qunobackend.domain.tag.TagNotFoundException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ListRelatedTagsUseCaseTest {
    private val tagRepository = InMemoryTagRepository()
    private val tagStatsRepository = InMemoryTagStatsRepository()
    private val useCase = ListRelatedTagsUseCase(tagRepository, tagStatsRepository)

    @Test
    fun `resolves the repository's ranked related tag ids`() {
        val tagId = requireNotNull(tagRepository.save(Tag.create("Kotlin")).id)
        val springId = requireNotNull(tagRepository.save(Tag.create("Spring Boot")).id)
        tagStatsRepository.relatedTagIds = listOf(springId)

        val result = useCase.execute(tagId)

        assertEquals(listOf("Spring Boot"), result.map { it.name })
    }

    @Test
    fun `silently skips related tag ids that no longer resolve`() {
        val tagId = requireNotNull(tagRepository.save(Tag.create("Kotlin")).id)
        tagStatsRepository.relatedTagIds = listOf(999L)

        assertTrue(useCase.execute(tagId).isEmpty())
    }

    @Test
    fun `rejects a tag that does not exist`() {
        assertFailsWith<TagNotFoundException> { useCase.execute(999L) }
    }
}
