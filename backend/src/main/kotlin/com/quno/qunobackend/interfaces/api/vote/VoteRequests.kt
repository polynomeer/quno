package com.quno.qunobackend.interfaces.api.vote

/** [value] must be 1 or -1 — enforced in the domain (Vote.cast), not Bean Validation, since
 * @Min/@Max can't express "either -1 or 1" (would also allow 0). */
data class CastVoteRequest(
    val value: Int,
)
