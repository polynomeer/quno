package com.quno.qunobackend.application.tag.usecase

import com.quno.qunobackend.domain.tag.Tag
import com.quno.qunobackend.domain.tag.TagNotFoundException
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FollowTagUseCaseTest {
    private val tagRepository = InMemoryTagRepository()
    private val userTagFollowRepository = InMemoryUserTagFollowRepository()
    private val followUseCase = FollowTagUseCase(tagRepository, userTagFollowRepository)
    private val unfollowUseCase = UnfollowTagUseCase(userTagFollowRepository)

    @Test
    fun `follows an existing tag`() {
        val tag = tagRepository.save(Tag.create("kotlin"))

        followUseCase.execute(userId = 1L, tagId = requireNotNull(tag.id))

        assertTrue(userTagFollowRepository.isFollowing(1L, tag.id))
    }

    @Test
    fun `following twice stays idempotent`() {
        val tag = tagRepository.save(Tag.create("kotlin"))

        followUseCase.execute(userId = 1L, tagId = requireNotNull(tag.id))
        followUseCase.execute(userId = 1L, tagId = tag.id)

        assertTrue(userTagFollowRepository.findFollowedTagIds(1L).size == 1)
    }

    @Test
    fun `rejects following a tag that does not exist`() {
        assertFailsWith<TagNotFoundException> { followUseCase.execute(userId = 1L, tagId = 999L) }
    }

    @Test
    fun `unfollow clears the follow`() {
        val tag = tagRepository.save(Tag.create("kotlin"))
        followUseCase.execute(userId = 1L, tagId = requireNotNull(tag.id))

        unfollowUseCase.execute(userId = 1L, tagId = tag.id)

        assertFalse(userTagFollowRepository.isFollowing(1L, tag.id))
    }
}
