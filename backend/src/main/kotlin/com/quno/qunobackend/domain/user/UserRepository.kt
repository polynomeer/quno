package com.quno.qunobackend.domain.user

/** Port implemented by infrastructure/persistence/jpa/adapter/UserRepositoryAdapter. */
interface UserRepository {
    fun save(user: User): User
    fun findById(id: Long): User?
    fun findByEmail(email: String): User?
    fun existsByEmail(email: String): Boolean
    fun existsByNickname(nickname: String): Boolean
}
