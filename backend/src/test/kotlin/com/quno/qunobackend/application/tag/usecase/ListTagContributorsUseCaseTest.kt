package com.quno.qunobackend.application.tag.usecase

import com.quno.qunobackend.domain.tag.Tag
import com.quno.qunobackend.domain.tag.TagContributor
import com.quno.qunobackend.domain.tag.TagNotFoundException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ListTagContributorsUseCaseTest {
    private val tagRepository = InMemoryTagRepository()
    private val tagStatsRepository = InMemoryTagStatsRepository()
    private val useCase = ListTagContributorsUseCase(tagRepository, tagStatsRepository)

    @Test
    fun `returns the repository's ranked contributors`() {
        val tagId = requireNotNull(tagRepository.save(Tag.create("Kotlin")).id)
        tagStatsRepository.contributors = listOf(
            TagContributor(userId = 1L, nickname = "alice", answerCount = 5),
            TagContributor(userId = 2L, nickname = "bob", answerCount = 3),
        )

        val result = useCase.execute(tagId)

        assertEquals(listOf("alice", "bob"), result.map { it.nickname })
        assertEquals(listOf(5L, 3L), result.map { it.answerCount })
    }

    @Test
    fun `rejects a tag that does not exist`() {
        assertFailsWith<TagNotFoundException> { useCase.execute(999L) }
    }
}
