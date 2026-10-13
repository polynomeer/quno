package com.quno.qunobackend.domain.user

/** Port implemented by infrastructure/persistence/jpa/adapter/UserRepositoryAdapter. */
interface UserRepository {
    fun save(user: User): User
    fun findById(id: Long): User?
    fun findByEmail(email: String): User?
    fun findByNickname(nickname: String): User?
    fun existsByEmail(email: String): Boolean
    fun existsByNickname(nickname: String): Boolean

    /** Batched id → nickname for list responses (ADR-0064). Unknown ids are absent from the map. */
    fun findNicknamesByIds(ids: Collection<Long>): Map<Long, String>
}
