package com.quno.qunobackend.domain.question

/**
 * Who may see a question (Phase 79, ADR-0085). ORG questions belong to one *verified*
 * organization — membership there only comes from company email verification, so "members only"
 * actually holds. Community organizations anyone can join can't own one.
 */
enum class QuestionVisibility { PUBLIC, ORG }

/**
 * The person looking, as visibility rules need them: their id (null = anonymous) and the
 * organizations they belong to. Resolved once per request and passed to everything that lists or
 * shows questions, so no read path can forget who is asking.
 */
data class QuestionViewer(val userId: Long?, val organizationIds: Set<Long>) {
    /** PUBLIC, the author's own, or an ORG question of one of the viewer's organizations. */
    fun canSee(question: Question): Boolean = canSee(question.visibility, question.organizationId, question.authorId)

    fun canSee(visibility: QuestionVisibility, organizationId: Long?, authorId: Long): Boolean = visibility == QuestionVisibility.PUBLIC ||
        (userId != null && authorId == userId) ||
        (organizationId != null && organizationId in organizationIds)

    /** For SQL `= ANY(...)`/`IN (...)`: never empty, so the query shape doesn't change with membership. */
    fun organizationIdsForSql(): List<Long> = organizationIds.toList().ifEmpty { listOf(NO_ORGANIZATION) }

    /** For SQL comparisons against author_id: -1 matches no one. */
    fun userIdForSql(): Long = userId ?: NO_USER

    companion object {
        val ANONYMOUS = QuestionViewer(null, emptySet())
        private const val NO_ORGANIZATION = -1L
        private const val NO_USER = -1L
    }
}

/** Only members of a verified organization can ask it a private question. */
class PrivateQuestionNotAllowedException(message: String) : RuntimeException(message)
