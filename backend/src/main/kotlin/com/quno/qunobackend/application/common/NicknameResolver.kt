package com.quno.qunobackend.application.common

import com.quno.qunobackend.domain.user.UserRepository
import org.springframework.stereotype.Component

/**
 * One batched lookup that turns the user ids in a list response into nicknames (ADR-0064), so a
 * screen shows "devGuru" instead of "사용자 #26". List controllers call it once per response after
 * their use case returns — the use cases and their results stay nickname-free, and a list of N
 * items costs one query, not N.
 *
 * A withdrawn user's row is kept with an anonymized nickname (ADR-0046), so they still resolve.
 */
@Component
class NicknameResolver(
    private val userRepository: UserRepository,
) {
    fun nicknamesOf(userIds: Iterable<Long?>): Map<Long, String> = userRepository.findNicknamesByIds(userIds.filterNotNull().toSet())
}
