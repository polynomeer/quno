package com.quno.qunobackend.domain.comment

interface CommentVersionRepository {
    fun save(version: CommentVersion): CommentVersion

    fun findAllByCommentIdOrderByVersionNumberAsc(commentId: Long): List<CommentVersion>
}
