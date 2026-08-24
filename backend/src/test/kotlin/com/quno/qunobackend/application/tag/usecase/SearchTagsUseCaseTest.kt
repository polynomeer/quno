package com.quno.qunobackend.application.tag.usecase

import com.quno.qunobackend.domain.tag.Tag
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class SearchTagsUseCaseTest {
    private val tagRepository = InMemoryTagRepository()
    private val useCase = SearchTagsUseCase(tagRepository)

    @Test
    fun `filters tags by a case-insensitive name match`() {
        tagRepository.save(Tag.create("Kotlin"))
        tagRepository.save(Tag.create("Java"))

        val result = useCase.execute(query = "kot")

        assertEquals(listOf("Kotlin"), result.map { it.name })
    }

    @Test
    fun `returns all active tags when no query is given`() {
        tagRepository.save(Tag.create("Kotlin"))
        tagRepository.save(Tag.create("Java"))

        val result = useCase.execute(query = null)

        assertEquals(listOf("Java", "Kotlin"), result.map { it.name }.sorted())
    }
}
