package com.quno.qunobackend.infrastructure.persistence.jpa.adapter

import com.quno.qunobackend.domain.user.User
import com.quno.qunobackend.domain.user.UserRepository
import com.quno.qunobackend.infrastructure.persistence.jpa.entity.UserJpaEntity
import com.quno.qunobackend.infrastructure.persistence.jpa.repository.UserJpaRepository
import org.springframework.stereotype.Component

@Component
class UserRepositoryAdapter(
    private val jpaRepository: UserJpaRepository,
) : UserRepository {

    override fun save(user: User): User {
        val entity = UserJpaEntity(
            id = user.id,
            email = user.email,
            nickname = user.nickname,
            passwordHash = user.passwordHash,
            isActive = user.isActive,
            role = user.role,
            acceptsDirectAsk = user.acceptsDirectAsk,
            createdAt = user.createdAt,
            updatedAt = user.updatedAt,
        )
        return jpaRepository.save(entity).toDomain()
    }

    override fun findById(id: Long): User? = jpaRepository.findById(id).orElse(null)?.toDomain()

    override fun findByEmail(email: String): User? = jpaRepository.findByEmail(email)?.toDomain()

    override fun findByNickname(nickname: String): User? = jpaRepository.findByNickname(nickname)?.toDomain()

    override fun existsByEmail(email: String): Boolean = jpaRepository.existsByEmail(email)

    override fun existsByNickname(nickname: String): Boolean = jpaRepository.existsByNickname(nickname)

    private fun UserJpaEntity.toDomain(): User = User.reconstitute(
        id = requireNotNull(id),
        email = email,
        nickname = nickname,
        passwordHash = passwordHash,
        isActive = isActive,
        role = role,
        acceptsDirectAsk = acceptsDirectAsk,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
