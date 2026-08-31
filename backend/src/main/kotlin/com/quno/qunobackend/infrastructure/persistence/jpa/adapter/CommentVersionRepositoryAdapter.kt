package com.quno.qunobackend.infrastructure.persistence.jpa.adapter

import com.quno.qunobackend.domain.comment.CommentVersion
import com.quno.qunobackend.domain.comment.CommentVersionRepository
import com.quno.qunobackend.infrastructure.persistence.jpa.entity.CommentVersionJpaEntity
import com.quno.qunobackend.infrastructure.persistence.jpa.repository.CommentVersionJpaRepository
import org.springframework.stereotype.Component

@Component
class CommentVersionRepositoryAdapter(
    private val jpaRepository: CommentVersionJpaRepository,
) : CommentVersionRepository {

    override fun save(version: CommentVersion): CommentVersion {
        val entity = CommentVersionJpaEntity(
            id = version.id,
            commentId = version.commentId,
            versionNumber = version.versionNumber,
            body = version.body,
            createdAt = version.createdAt,
        )
        return jpaRepository.save(entity).toDomain()
    }

    override fun findAllByCommentIdOrderByVersionNumberAsc(commentId: Long): List<CommentVersion> =
        jpaRepository.findAllByCommentIdOrderByVersionNumberAsc(commentId).map { it.toDomain() }

    private fun CommentVersionJpaEntity.toDomain(): CommentVersion = CommentVersion.reconstitute(
        id = requireNotNull(id),
        commentId = commentId,
        versionNumber = versionNumber,
        body = body,
        createdAt = createdAt,
    )
}
