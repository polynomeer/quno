package com.quno.qunobackend.application.report.usecase

import com.quno.qunobackend.domain.report.ModeratorAccessDeniedException
import com.quno.qunobackend.domain.user.Role
import com.quno.qunobackend.domain.user.UserNotFoundException
import com.quno.qunobackend.domain.user.UserRepository

/** Role is checked fresh on every call (ADR-0028) — no JWT claim, so a demotion takes effect
 * on the very next request instead of waiting out the access token's lifetime. */
internal fun UserRepository.requireModerator(userId: Long) {
    val user = findById(userId) ?: throw UserNotFoundException(userId)
    if (user.role != Role.MODERATOR) throw ModeratorAccessDeniedException(userId)
}
