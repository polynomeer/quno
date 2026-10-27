package com.quno.qunobackend.integration

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultMatcher
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper
import java.util.UUID
import kotlin.test.assertEquals

/**
 * Phase 63 (ADR-0076) through real HTTP and Postgres: parsing stores nothing, only the entries
 * sent with the question are kept, a revision without `dependencies` keeps them, `[]` clears
 * them, and a fork inherits them.
 */
@SpringBootTest
@AutoConfigureMockMvc
class StackFingerprintLifecycleE2ETest {

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
        questionIds.reversed().forEach { id ->
            jdbcTemplate.update("DELETE FROM notifications WHERE question_id = ?", id)
            jdbcTemplate.update("DELETE FROM outbox_events WHERE aggregate_id = ?", id)
            jdbcTemplate.update("UPDATE questions SET latest_version_id = NULL, origin_question_id = NULL WHERE id = ?", id)
            jdbcTemplate.update("DELETE FROM question_versions WHERE question_id = ?", id)
        }
        questionIds.reversed().forEach { id -> jdbcTemplate.update("DELETE FROM questions WHERE id = ?", id) }
        userIds.forEach { id -> jdbcTemplate.update("DELETE FROM notifications WHERE user_id = ?", id) }
        userIds.forEach { id -> jdbcTemplate.update("DELETE FROM users WHERE id = ?", id) }
    }

    @Test
    fun `parse previews without storing, and only the kept entries follow the question`() {
        val (userId, token) = signUpAndLogin("stack")
        userIds += userId
        val before = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM question_dependencies", Long::class.java)

        val parsed = send(
            "/api/v1/stack-fingerprints/parse",
            token,
            mapOf("fileName" to "build.gradle.kts", "content" to "plugins { id(\"org.springframework.boot\") version \"4.0.8\" }\ndependencies { implementation(\"io.r2dbc:r2dbc-pool:1.0.2\")\n implementation(\"com.acme:billing-common:0.14.0\") }"),
        )
        assertEquals("GRADLE", parsed["format"])
        assertEquals(3, (parsed["dependencies"] as List<*>).size)
        assertEquals(before, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM question_dependencies", Long::class.java))

        // The author drops the in-house package before posting.
        val kept = listOf(
            mapOf("ecosystem" to "GRADLE_PLUGIN", "name" to "org.springframework.boot", "version" to "4.0.8"),
            mapOf("ecosystem" to "MAVEN", "name" to "io.r2dbc:r2dbc-pool", "version" to "1.0.2"),
        )
        val questionId = (send("/api/v1/questions", token, mapOf("title" to "Pool exhausted", "body" to "b", "dependencies" to kept), status().isCreated)["id"] as Number).toLong()
        questionIds += questionId
        assertEquals(kept, dependenciesOf(questionId, token))

        send("/api/v1/questions/$questionId/versions", token, mapOf("title" to "Pool exhausted", "body" to "b2"))
        assertEquals(kept, dependenciesOf(questionId, token))

        val forkId = (send("/api/v1/questions/$questionId/fork", token, emptyMap<String, Any>(), status().isCreated)["id"] as Number).toLong()
        questionIds += forkId
        assertEquals(kept, dependenciesOf(forkId, token))

        send("/api/v1/questions/$questionId/versions", token, mapOf("title" to "Pool exhausted", "body" to "b3", "dependencies" to emptyList<Any>()))
        assertEquals(emptyList<Any>(), dependenciesOf(questionId, token))
    }

    @Test
    fun `invalid entries are a 400, not a 500`() {
        val (userId, token) = signUpAndLogin("stackbad")
        userIds += userId

        send("/api/v1/questions", token, mapOf("title" to "t", "body" to "b", "dependencies" to listOf(mapOf("ecosystem" to "PYPI", "name" to "x"))), status().isBadRequest)
        val tooMany = (1..201).map { mapOf("ecosystem" to "NPM", "name" to "p$it") }
        send("/api/v1/questions", token, mapOf("title" to "t", "body" to "b", "dependencies" to tooMany), status().isBadRequest)
        send("/api/v1/questions", token, mapOf("title" to "t", "body" to "b", "dependencies" to listOf(mapOf("ecosystem" to "NPM", "name" to " "))), status().isBadRequest)
    }

    private fun dependenciesOf(questionId: Long, token: String): List<*> {
        val json = mockMvc.perform(get("/api/v1/questions/$questionId").header("Authorization", "Bearer $token"))
            .andExpect(status().isOk).andReturn().response.contentAsString
        return readMap(json)["dependencies"] as List<*>
    }

    private fun send(path: String, token: String?, body: Map<String, Any?>, expected: ResultMatcher = status().isOk): Map<String, Any?> {
        val request = post(path).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body))
        token?.let { request.header("Authorization", "Bearer $it") }
        val json = mockMvc.perform(request).andExpect(expected).andReturn().response.contentAsString
        return if (json.isBlank()) emptyMap() else readMap(json)
    }

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
