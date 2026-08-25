package com.quno.qunobackend.domain.review

import java.time.Instant

enum class ReviewRequestStatus { OPEN, ADDRESSED }

/**
 * One reviewer's independent request for more info on a Question (QPR "Review" — see
 * docs/architecture/decisions/0012-qpr-multi-reviewer-thread-model.md). Several of these
 * can be open on the same question at once; each is addressed independently via re-request
 * (PLAN.md 5.3), not by a single global flag on Question.
 */
class ReviewRequest private constructor(
    val id: Long?,
    val questionId: Long,
    val requestedBy: Long,
    val message: String,
    val status: ReviewRequestStatus,
    /** The question's latest version number when this request was created — re-request
     *  requires a revision past this point (PLAN.md 5.3). */
    val questionVersionNumberAtRequest: Int,
    val createdAt: Instant,
    val addressedAt: Instant?,
) {
    fun addressed(): ReviewRequest {
        check(status == ReviewRequestStatus.OPEN) { "review request is already addressed" }
        return ReviewRequest(
            id, questionId, requestedBy, message, ReviewRequestStatus.ADDRESSED, questionVersionNumberAtRequest, createdAt, Instant.now(),
        )
    }

    companion object {
        fun request(questionId: Long, requestedBy: Long, message: String, questionVersionNumberAtRequest: Int): ReviewRequest {
            require(message.isNotBlank()) { "message must not be blank" }
            return ReviewRequest(
                id = null,
                questionId = questionId,
                requestedBy = requestedBy,
                message = message,
                status = ReviewRequestStatus.OPEN,
                questionVersionNumberAtRequest = questionVersionNumberAtRequest,
                createdAt = Instant.now(),
                addressedAt = null,
            )
        }

        fun reconstitute(
            id: Long,
            questionId: Long,
            requestedBy: Long,
            message: String,
            status: ReviewRequestStatus,
            questionVersionNumberAtRequest: Int,
            createdAt: Instant,
            addressedAt: Instant?,
        ): ReviewRequest = ReviewRequest(
            id, questionId, requestedBy, message, status, questionVersionNumberAtRequest, createdAt, addressedAt,
        )
    }
}
