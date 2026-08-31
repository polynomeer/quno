package com.quno.qunobackend.application.comment.usecase

import com.quno.qunobackend.domain.comment.CommentVersion
import com.quno.qunobackend.domain.comment.CommentVersionRepository

class InMemoryCommentVersionRepository : CommentVersionRepository {
    private val versions = mutableListOf<CommentVersion>()
    private var nextId = 1L

    override fun save(version: CommentVersion): CommentVersion {
        val saved = CommentVersion.reconstitute(
            id = nextId++,
            commentId = version.commentId,
            versionNumber = version.versionNumber,
            body = version.body,
            createdAt = version.createdAt,
        )
        versions.add(saved)
        return saved
    }

    override fun findAllByCommentIdOrderByVersionNumberAsc(commentId: Long): List<CommentVersion> =
        versions.filter { it.commentId == commentId }.sortedBy { it.versionNumber }
}
