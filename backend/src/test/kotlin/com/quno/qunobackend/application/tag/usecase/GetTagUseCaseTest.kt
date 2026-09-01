package com.quno.qunobackend.application.tag.usecase

import com.quno.qunobackend.domain.tag.Tag
import com.quno.qunobackend.domain.tag.TagNotFoundException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GetTagUseCaseTest {
    private val tagRepository = InMemoryTagRepository()
    private val useCase = GetTagUseCase(tagRepository)

    @Test
    fun `returns the tag by id`() {
        val saved = tagRepository.save(Tag.create("Kotlin"))

        val result = useCase.execute(requireNotNull(saved.id))

        assertEquals("Kotlin", result.name)
        assertEquals("kotlin", result.slug)
    }

    @Test
    fun `rejects a tag that does not exist`() {
        assertFailsWith<TagNotFoundException> { useCase.execute(999L) }
    }
}
