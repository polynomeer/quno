package com.quno.qunobackend.application.tag.usecase

import com.quno.qunobackend.domain.tag.Tag
import com.quno.qunobackend.domain.tag.TagNotFoundException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class UpdateTagDetailsUseCaseTest {
    private val tagRepository = InMemoryTagRepository()
    private val useCase = UpdateTagDetailsUseCase(tagRepository)

    @Test
    fun `sets description and docsUrl, persisted`() {
        val tagId = requireNotNull(tagRepository.save(Tag.create("Kotlin")).id)

        val result = useCase.execute(tagId, "A statically typed language", "https://kotlinlang.org/docs")

        assertEquals("A statically typed language", result.description)
        assertEquals("https://kotlinlang.org/docs", result.docsUrl)
        assertEquals("A statically typed language", tagRepository.findById(tagId)?.description)
    }

    @Test
    fun `clears an existing description by passing null`() {
        val tagId = requireNotNull(tagRepository.save(Tag.create("Kotlin")).id)
        useCase.execute(tagId, "old description", null)

        val result = useCase.execute(tagId, null, null)

        assertNull(result.description)
    }

    @Test
    fun `rejects a tag that does not exist`() {
        assertFailsWith<TagNotFoundException> { useCase.execute(999L, "desc", null) }
    }
}
