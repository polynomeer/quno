package com.quno.qunobackend.infrastructure.security

import com.quno.qunobackend.domain.token.TokenScope
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TokenScopePolicyTest {
    private val read = setOf(TokenScope.READ)
    private val ask = setOf(TokenScope.ASK)
    private val both = setOf(TokenScope.READ, TokenScope.ASK)

    @Test
    fun `READ opens every GET and only the storing-nothing POSTs`() {
        assertTrue(TokenScopePolicy.allows(read, "GET", "/api/v1/search"))
        assertTrue(TokenScopePolicy.allows(read, "GET", "/api/v1/me/stack"))
        assertTrue(TokenScopePolicy.allows(read, "POST", "/api/v1/error-signatures/preview"))
        assertTrue(TokenScopePolicy.allows(read, "POST", "/api/v1/stack-fingerprints/parse"))
        assertFalse(TokenScopePolicy.allows(read, "POST", "/api/v1/questions"))
    }

    @Test
    fun `ASK opens asking and revising questions, nothing else`() {
        assertTrue(TokenScopePolicy.allows(ask, "POST", "/api/v1/questions"))
        assertTrue(TokenScopePolicy.allows(ask, "POST", "/api/v1/questions/42/versions"))
        assertFalse(TokenScopePolicy.allows(ask, "GET", "/api/v1/search"))
        assertFalse(TokenScopePolicy.allows(ask, "POST", "/api/v1/questions/42/answers"))
        assertFalse(TokenScopePolicy.allows(ask, "POST", "/api/v1/questions/42/fork"))
    }

    @Test
    fun `no scope reaches token management, votes, deletes or other verbs`() {
        assertFalse(TokenScopePolicy.allows(both, "GET", "/api/v1/me/tokens"))
        assertFalse(TokenScopePolicy.allows(both, "DELETE", "/api/v1/me/tokens/3"))
        assertFalse(TokenScopePolicy.allows(both, "POST", "/api/v1/me/tokens"))
        assertFalse(TokenScopePolicy.allows(both, "POST", "/api/v1/votes"))
        assertFalse(TokenScopePolicy.allows(both, "PUT", "/api/v1/me/stack"))
        assertFalse(TokenScopePolicy.allows(both, "DELETE", "/api/v1/questions/42/watch"))
    }
}
