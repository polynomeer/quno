package com.quno.qunobackend.integration

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultMatcher
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Phase 65.1·65.4 (ADR-0079) through real HTTP and Postgres: tokens are issued and managed only
 * from a browser session, the secret is shown once and stored as a hash, scopes gate what a token
 * reaches, a dead token is a 401 even on public endpoints, and each token has its own bucket.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PersonalAccessTokenLifecycleE2ETest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    private val questionIds = mutableListOf<Long>()
    private val userIds = mutableListOf<Long>()

    @AfterEach
    fun cleanUp() {
        questionIds.forEach { id ->
            jdbcTemplate.update("DELETE FROM notifications WHERE question_id = ?", id)
            jdbcTemplate.update("DELETE FROM outbox_events WHERE aggregate_id = ?", id)
            jdbcTemplate.update("UPDATE questions SET latest_version_id = NULL WHERE id = ?", id)
            jdbcTemplate.update("DELETE FROM question_versions WHERE question_id = ?", id)
            jdbcTemplate.update("DELETE FROM questions WHERE id = ?", id)
        }
        userIds.forEach { id -> jdbcTemplate.update("DELETE FROM notifications WHERE user_id = ?", id) }
        userIds.forEach { id -> jdbcTemplate.update("DELETE FROM users WHERE id = ?", id) }
    }

    @Test
    fun `a token is issued once, stored as a hash, and does only what its scopes allow`() {
        val (userId, jwt) = signUpAndLogin("pat")
        userIds += userId

        val issued = call(HttpMethod.POST, "/api/v1/me/tokens", jwt, mapOf("name" to "laptop CLI", "scopes" to listOf("READ")), status().isCreated)
        val secret = issued["secret"] as String
        assertTrue(secret.startsWith("quno_pat_"))
        @Suppress("UNCHECKED_CAST")
        val token = issued["token"] as Map<String, Any?>
        assertEquals(secret.take(12), token["displayPrefix"])
        assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM personal_access_tokens WHERE user_id = ? AND token_hash <> ?", Int::class.java, userId, secret))

        val listed = callList("/api/v1/me/tokens", jwt)
        assertEquals(listOf("laptop CLI"), listed.map { it["name"] })
        assertFalse(listed.single().containsKey("secret"))

        call(HttpMethod.GET, "/api/v1/me/stack", secret)
        call(HttpMethod.POST, "/api/v1/stack-fingerprints/parse", secret, mapOf("content" to "{}"))
        call(HttpMethod.POST, "/api/v1/questions", secret, mapOf("title" to "t", "body" to "b"), status().isForbidden)
        call(HttpMethod.GET, "/api/v1/me/tokens", secret, expected = status().isForbidden)
        call(HttpMethod.POST, "/api/v1/me/tokens", secret, mapOf("name" to "escalate", "scopes" to listOf("ASK")), status().isForbidden)

        val asker = call(HttpMethod.POST, "/api/v1/me/tokens", jwt, mapOf("name" to "agent", "scopes" to listOf("ASK"), "lifetimeDays" to 30), status().isCreated)["secret"] as String
        val questionId = (call(HttpMethod.POST, "/api/v1/questions", asker, mapOf("title" to "Asked from the CLI", "body" to "b"), status().isCreated)["id"] as Number).toLong()
        questionIds += questionId
        assertEquals(userId, jdbcTemplate.queryForObject("SELECT author_id FROM questions WHERE id = ?", Long::class.java, questionId))
        call(HttpMethod.POST, "/api/v1/questions/$questionId/answers", asker, mapOf("body" to "self answer"), status().isForbidden)

        val tokenId = (token["id"] as Number).toLong()
        call(HttpMethod.DELETE, "/api/v1/me/tokens/$tokenId", jwt, expected = status().isNoContent)
        call(HttpMethod.GET, "/api/v1/search?q=x", secret, expected = status().isUnauthorized)
        assertTrue(callList("/api/v1/me/tokens", jwt).single { it["id"] == token["id"] }["revokedAt"] != null)
    }

    @Test
    fun `bad requests, other people's tokens and the active-token cap`() {
        val (userId, jwt) = signUpAndLogin("patrules")
        val (otherId, otherJwt) = signUpAndLogin("patother")
        userIds += listOf(userId, otherId)

        call(HttpMethod.POST, "/api/v1/me/tokens", jwt, mapOf("name" to "x", "scopes" to listOf("READ"), "lifetimeDays" to 7), status().isBadRequest)
        call(HttpMethod.POST, "/api/v1/me/tokens", jwt, mapOf("name" to "x", "scopes" to emptyList<String>()), status().isBadRequest)
        call(HttpMethod.POST, "/api/v1/me/tokens", jwt, mapOf("name" to "x", "scopes" to listOf("ADMIN")), status().isBadRequest)
        call(HttpMethod.GET, "/api/v1/questions/1", "quno_pat_not-a-real-token", expected = status().isUnauthorized)

        val ids = (1..10).map {
            ((call(HttpMethod.POST, "/api/v1/me/tokens", jwt, mapOf("name" to "t$it", "scopes" to listOf("READ")), status().isCreated)["token"] as Map<*, *>)["id"] as Number).toLong()
        }
        call(HttpMethod.POST, "/api/v1/me/tokens", jwt, mapOf("name" to "eleventh", "scopes" to listOf("READ")), status().isConflict)
        call(HttpMethod.DELETE, "/api/v1/me/tokens/${ids.first()}", otherJwt, expected = status().isNotFound)
        call(HttpMethod.DELETE, "/api/v1/me/tokens/${ids.first()}", jwt, expected = status().isNoContent)
        call(HttpMethod.POST, "/api/v1/me/tokens", jwt, mapOf("name" to "after revoke", "scopes" to listOf("READ")), status().isCreated)
        assertNull(callList("/api/v1/me/tokens", otherJwt).firstOrNull())
    }

    @Test
    fun `each token gets its own bucket`() {
        val (userId, jwt) = signUpAndLogin("patrate")
        userIds += userId
        val secret = call(HttpMethod.POST, "/api/v1/me/tokens", jwt, mapOf("name" to "busy", "scopes" to listOf("READ")), status().isCreated)["secret"] as String
        val fresh = call(HttpMethod.POST, "/api/v1/me/tokens", jwt, mapOf("name" to "idle", "scopes" to listOf("READ")), status().isCreated)["secret"] as String

        repeat(120) { call(HttpMethod.GET, "/api/v1/me/stack", secret) }
        call(HttpMethod.GET, "/api/v1/me/stack", secret, expected = status().isTooManyRequests)
        call(HttpMethod.GET, "/api/v1/me/stack", fresh)
        call(HttpMethod.GET, "/api/v1/me/stack", jwt)
    }

    private fun call(method: HttpMethod, path: String, token: String?, body: Map<String, Any?>? = null, expected: ResultMatcher = status().isOk): Map<String, Any?> {
        val builder = request(method, path)
        body?.let { builder.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(it)) }
        token?.let { builder.header("Authorization", "Bearer $it") }
        val json = mockMvc.perform(builder).andExpect(expected).andReturn().response.contentAsString
        return if (json.isBlank() || !json.trimStart().startsWith("{")) emptyMap() else readMap(json)
    }

    private fun callList(path: String, token: String): List<Map<String, Any?>> {
        val json = mockMvc.perform(request(HttpMethod.GET, path).header("Authorization", "Bearer $token")).andExpect(status().isOk).andReturn().response.contentAsString
        @Suppress("UNCHECKED_CAST")
        return objectMapper.readValue(json, List::class.java) as List<Map<String, Any?>>
    }

    private fun send(path: String, token: String?, body: Map<String, Any?>, expected: ResultMatcher = status().isOk): Map<String, Any?> = call(HttpMethod.POST, path, token, body, expected)

    private fun signUpAndLogin(prefix: String): Pair<Long, String> {
        val email = "$prefix-${UUID.randomUUID()}@example.com"
        val nickname = "$prefix${System.nanoTime() % 1_000_000}"
        val signUp = send("/api/v1/auth/signup", null, mapOf("email" to email, "nickname" to nickname, "password" to "password123"), status().isCreated)
        val login = send("/api/v1/auth/login", null, mapOf("email" to email, "password" to "password123"))
        return (signUp["id"] as Number).toLong() to (login["accessToken"] as String)
    }

    @Suppress("UNCHECKED_CAST")
    private fun readMap(json: String): Map<String, Any?> = objectMapper.readValue(json, Map::class.java) as Map<String, Any?>
}
