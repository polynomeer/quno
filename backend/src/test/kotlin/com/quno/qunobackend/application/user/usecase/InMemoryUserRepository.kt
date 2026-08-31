package com.quno.qunobackend.application.user.usecase

import com.quno.qunobackend.domain.user.Role
import com.quno.qunobackend.domain.user.User
import com.quno.qunobackend.domain.user.UserRepository

class InMemoryUserRepository : UserRepository {
    private val usersById = mutableMapOf<Long, User>()
    private var nextId = 1L

    override fun save(user: User): User {
        val saved = if (user.id == null) {
            User.reconstitute(
                id = nextId++,
                email = user.email,
                nickname = user.nickname,
                passwordHash = user.passwordHash,
                isActive = user.isActive,
                role = user.role,
                createdAt = user.createdAt,
                updatedAt = user.updatedAt,
            )
        } else {
            user
        }
        usersById[requireNotNull(saved.id)] = saved
        return saved
    }

    override fun findById(id: Long): User? = usersById[id]
    override fun findByEmail(email: String): User? = usersById.values.find { it.email == email }
    override fun existsByEmail(email: String): Boolean = usersById.values.any { it.email == email }
    override fun existsByNickname(nickname: String): Boolean = usersById.values.any { it.nickname == nickname }

    /** Test-only stand-in for the DB-direct promotion ADR-0028 relies on (no promotion API exists). */
    fun promoteToModerator(userId: Long) {
        val user = requireNotNull(usersById[userId])
        usersById[userId] = User.reconstitute(
            id = userId,
            email = user.email,
            nickname = user.nickname,
            passwordHash = user.passwordHash,
            isActive = user.isActive,
            role = Role.MODERATOR,
            createdAt = user.createdAt,
            updatedAt = user.updatedAt,
        )
    }
}
