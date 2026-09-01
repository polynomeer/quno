package com.quno.qunobackend.infrastructure.persistence.jpa.repository

import com.quno.qunobackend.infrastructure.persistence.jpa.entity.QuestionJpaEntity
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.Repository
import org.springframework.data.repository.query.Param
import java.time.LocalDate

/**
 * Not a CRUD repository — only runs the two "which questions might be affected by a technology
 * release" queries behind Phase 21 (ADR-0033). "Content hasn't changed since the release" is the
 * same MAX(question_versions.created_at) signal FlowJpaRepository already uses for "reopened" —
 * `questions.updated_at` isn't usable here because it also moves on status-only changes
 * (accept, cluster join, ...) that aren't a content revision.
 */
interface VersionImpactJpaRepository : Repository<QuestionJpaEntity, Long> {

    @Query(
        value = """
            SELECT q.id AS questionId, q.author_id AS questionAuthorId
            FROM tags t
            JOIN question_tags qt ON qt.tag_id = t.id
            JOIN questions q ON q.id = qt.question_id AND q.deleted_at IS NULL
            JOIN (
                SELECT question_id, MAX(created_at) AS latest_content_at
                FROM question_versions
                GROUP BY question_id
            ) lc ON lc.question_id = q.id
            WHERE t.slug = :tagSlug AND t.deleted_at IS NULL
              AND q.status NOT IN ('RESOLVED', 'OUTDATED')
              AND lc.latest_content_at < :sinceDate
            ORDER BY q.id DESC
            LIMIT :limit
        """,
        nativeQuery = true,
    )
    fun findAffectedQuestions(
        @Param("tagSlug") tagSlug: String,
        @Param("sinceDate") sinceDate: LocalDate,
        @Param("limit") limit: Int,
    ): List<AffectedQuestionProjection>

    @Query(
        value = """
            SELECT
                q.id AS questionId,
                q.title AS questionTitle,
                tr.tag_slug AS tagSlug,
                tr.product_slug AS productSlug,
                tr.latest_version AS latestVersion,
                tr.latest_release_date AS latestReleaseDate
            FROM technology_releases tr
            JOIN tags t ON t.slug = tr.tag_slug AND t.deleted_at IS NULL
            JOIN question_tags qt ON qt.tag_id = t.id
            JOIN questions q ON q.id = qt.question_id AND q.deleted_at IS NULL
            JOIN (
                SELECT question_id, MAX(created_at) AS latest_content_at
                FROM question_versions
                GROUP BY question_id
            ) lc ON lc.question_id = q.id
            WHERE q.status NOT IN ('RESOLVED', 'OUTDATED')
              AND lc.latest_content_at < tr.latest_release_date
            ORDER BY tr.latest_release_date DESC, q.id DESC
            LIMIT :limit
        """,
        nativeQuery = true,
    )
    fun findVersionImpacts(@Param("limit") limit: Int): List<VersionImpactProjection>
}
