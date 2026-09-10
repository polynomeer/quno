package com.quno.qunobackend.domain.user

import java.time.Instant

/**
 * Aggregate root for a Quno account. Password hashing happens outside this class
 * (application layer, via PasswordEncoder); this class only ever holds the hash.
 */
class User private constructor(
    val id: Long?,
    val email: String,
    val nickname: String,
    val passwordHash: String,
    val isActive: Boolean,
    val role: Role,
    /** Opt-in, defaults to false (Phase 22, ADR-0034) — a request is refused rather than merely
     * hidden when this is false, matching the original brainstorm's "expert sets whether they
     * accept Direct Ask". */
    val acceptsDirectAsk: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    fun updateDirectAskSettings(accepts: Boolean): User = User(id, email, nickname, passwordHash, isActive, role, accepts, createdAt, Instant.now())

    /**
     * 개인정보 삭제 요청(회원 탈퇴)을 반영한다 — Row를 지우지 않고 PII만 익명화한다(ADR-0046).
     * 이미 작성한 질문/답변/댓글은 다른 사용자에게 여전히 가치가 있고 FK가 이 User row를 그대로
     * 가리키므로, 사후 조회 시 자동으로 "탈퇴한 사용자N"으로 보인다. 이메일/닉네임 UNIQUE 제약을
     * 지키기 위해 id를 접미사로 붙인다. 비밀번호 해시는 호출자(application 계층)가 무작위 값으로
     * 새로 인코딩해서 넘긴다 — 이 클래스는 해시를 만들지 않는다는 기존 원칙을 그대로 따른다.
     */
    fun withdraw(randomPasswordHash: String): User {
        val withdrawnId = requireNotNull(id) { "cannot withdraw a user that hasn't been persisted yet" }
        return User(
            id = withdrawnId,
            email = "withdrawn-user-$withdrawnId@quno.invalid",
            nickname = "탈퇴한 사용자$withdrawnId",
            passwordHash = randomPasswordHash,
            isActive = false,
            role = role,
            acceptsDirectAsk = false,
            createdAt = createdAt,
            updatedAt = Instant.now(),
        )
    }

    companion object {
        fun register(email: String, nickname: String, passwordHash: String): User {
            require(email.isNotBlank()) { "email must not be blank" }
            require(nickname.isNotBlank()) { "nickname must not be blank" }
            require(passwordHash.isNotBlank()) { "passwordHash must not be blank" }
            val now = Instant.now()
            return User(
                id = null,
                email = email,
                nickname = nickname,
                passwordHash = passwordHash,
                isActive = true,
                role = Role.USER,
                acceptsDirectAsk = false,
                createdAt = now,
                updatedAt = now,
            )
        }

        fun reconstitute(
            id: Long,
            email: String,
            nickname: String,
            passwordHash: String,
            isActive: Boolean,
            role: Role,
            acceptsDirectAsk: Boolean,
            createdAt: Instant,
            updatedAt: Instant,
        ): User = User(id, email, nickname, passwordHash, isActive, role, acceptsDirectAsk, createdAt, updatedAt)
    }
}
