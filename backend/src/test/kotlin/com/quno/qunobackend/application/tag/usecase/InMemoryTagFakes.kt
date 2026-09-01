package com.quno.qunobackend.application.tag.usecase

import com.quno.qunobackend.domain.tag.QuestionTagRepository
import com.quno.qunobackend.domain.tag.Tag
import com.quno.qunobackend.domain.tag.TagRepository
import com.quno.qunobackend.domain.tag.UserTagFollowRepository

class InMemoryTagRepository : TagRepository {
    private val byId = mutableMapOf<Long, Tag>()
    private var nextId = 1L

    override fun save(tag: Tag): Tag {
        val saved = if (tag.id == null) {
            Tag.reconstitute(nextId++, tag.name, tag.slug, tag.description, tag.docsUrl, tag.deletedAt, tag.createdAt)
        } else {
            tag
        }
        byId[requireNotNull(saved.id)] = saved
        return saved
    }

    override fun findById(id: Long): Tag? = byId[id]?.takeIf { it.deletedAt == null }

    override fun findBySlug(slug: String): Tag? = byId.values.find { it.slug == slug && it.deletedAt == null }

    override fun search(query: String?, limit: Int): List<Tag> =
        byId.values
            .filter { it.deletedAt == null && (query.isNullOrBlank() || it.name.contains(query, ignoreCase = true)) }
            .sortedBy { it.name }
            .take(limit)
}

class InMemoryQuestionTagRepository(private val tagRepository: InMemoryTagRepository) : QuestionTagRepository {
    private val links = mutableSetOf<Pair<Long, Long>>()

    override fun attach(questionId: Long, tagId: Long) {
        links += questionId to tagId
    }

    override fun findTagsByQuestionId(questionId: Long): List<Tag> =
        links.filter { it.first == questionId }.mapNotNull { tagRepository.findById(it.second) }
}

class InMemoryUserTagFollowRepository : UserTagFollowRepository {
    private val follows = mutableSetOf<Pair<Long, Long>>()

    override fun follow(userId: Long, tagId: Long) {
        follows += userId to tagId
    }

    override fun unfollow(userId: Long, tagId: Long) {
        follows -= userId to tagId
    }

    override fun isFollowing(userId: Long, tagId: Long): Boolean = (userId to tagId) in follows

    override fun findFollowedTagIds(userId: Long): List<Long> = follows.filter { it.first == userId }.map { it.second }
}
