package com.quno.qunobackend.domain.recommendation

/**
 * Port implemented by infrastructure/persistence/jpa/adapter/RecommendationRepositoryAdapter.
 * MVP scoring is intentionally simple and explainable — see
 * docs/architecture/domain-model.md#태그-팔로우-기반-추천-쿼리.
 */
interface RecommendationRepository {
    /**
     * Candidates are questions tagged with something the user follows (excluding their own
     * questions), scored by `matched_tag_count * 3 + min(answer_count, 5)`, most relevant first.
     */
    fun recommendQuestionIdsByTagFollows(userId: Long, limit: Int): List<Long>
}
