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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper
import java.util.UUID
import kotlin.test.assertEquals

/**
 * Phase 63.5 (ADR-0078) through real HTTP and Postgres: "내 스택" is replaced as a whole and
 * private, and Ward suggestions rank questions by shared packages while leaving out the user's
 * own questions and the ones already watched. Nothing is watched automatically.
 */
@SpringBootTest
@AutoConfigureMockMvc
class MyStackLifecycleE2ETest {

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
            jdbcTemplate.update("DELETE FROM watches WHERE question_id = ?", id)
            jdbcTemplate.update("DELETE FROM notifications WHERE question_id = ?", id)
            jdbcTemplate.update("DELETE FROM outbox_events WHERE aggregate_id = ?", id)
            jdbcTemplate.update("UPDATE questions SET latest_version_id = NULL WHERE id = ?", id)
            jdbcTemplate.update("DELETE FROM question_versions WHERE question_id = ?", id)
            jdbcTemplate.update("DELETE FROM questions WHERE id = ?", id)
        }
        userIds.forEach { id -> jdbcTemplate.update("DELETE FROM notifications WHERE user_id = ?", id) }
        userIds.forEach { id -> jdbcTemplate.update("DELETE FROM users WHERE id = ?", id) }
    }

    private val boot = mapOf("ecosystem" to "GRADLE_PLUGIN", "name" to "org.springframework.boot", "version" to "4.0.8")
    private val hikari = mapOf("ecosystem" to "MAVEN", "name" to "com.zaxxer:HikariCP", "version" to "6.3.0")

    @Test
    fun `the stack is replaced as a whole and starts empty`() {
        val (userId, token) = signUpAndLogin("mystack")
        userIds += userId

        assertEquals(emptyList<Any>(), stackOf(token))
        assertEquals(listOf(boot, hikari), replace(token, listOf(boot, hikari, boot))["dependencies"], "duplicates collapse")
        assertEquals(listOf(boot, hikari), stackOf(token))
        replace(token, emptyList())
        assertEquals(emptyList<Any>(), stackOf(token))

        replace(token, listOf(mapOf("ecosystem" to "PYPI", "name" to "django")), status().isBadRequest)
        replace(token, (1..201).map { mapOf("ecosystem" to "NPM", "name" to "p$it") }, status().isBadRequest)
        mockMvc.perform(get("/api/v1/me/stack")).andExpect(status().isUnauthorized)
    }

    @Test
    fun `suggests questions sharing packages, most shared first, never mine or already watched`() {
        val (meId, me) = signUpAndLogin("stackme")
        val (otherId, other) = signUpAndLogin("stackother")
        userIds += listOf(meId, otherId)
        replace(me, listOf(boot, hikari))

        // Versions differ from mine on purpose: matching is by package.
        val both = ask(other, "Pool leak", listOf(boot.plus("version" to "3.4.1"), hikari))
        val bootOnly = ask(other, "Actuator 404", listOf(boot))
        ask(other, "React hydration", listOf(mapOf("ecosystem" to "NPM", "name" to "react", "version" to "19.2.0")))
        ask(me, "My own boot question", listOf(boot))

        // The shared database may hold other questions with these packages; only this test's count.
        val suggestions = suggestionsOf(me).filter { (it["questionId"] as Number).toLong() in questionIds }
        assertEquals(listOf(both, bootOnly), suggestions.map { (it["questionId"] as Number).toLong() })
        assertEquals(listOf(boot.plus("version" to "3.4.1"), hikari), suggestions.first()["sharedDependencies"])
        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM watches WHERE user_id = ?", Int::class.java, meId))

        send("/api/v1/questions/$bootOnly/watch", me, emptyMap(), status().isNoContent)
        assertEquals(listOf(both), suggestionsOf(me).map { (it["questionId"] as Number).toLong() }.filter { it in questionIds })
    }

    private fun ask(token: String, title: String, dependencies: List<Map<String, Any?>>): Long {
        val id = (send("/api/v1/questions", token, mapOf("title" to title, "body" to "b", "dependencies" to dependencies), status().isCreated)["id"] as Number).toLong()
        questionIds += id
        return id
    }

    private fun replace(token: String, dependencies: List<Map<String, Any?>>, expected: ResultMatcher = status().isOk): Map<String, Any?> {
        val json = mockMvc.perform(
            put("/api/v1/me/stack").header("Authorization", "Bearer $token").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf("dependencies" to dependencies))),
        ).andExpect(expected).andReturn().response.contentAsString
        return if (json.isBlank()) emptyMap() else readMap(json)
    }

    private fun stackOf(token: String): List<*> {
        val json = mockMvc.perform(get("/api/v1/me/stack").header("Authorization", "Bearer $token")).andExpect(status().isOk).andReturn().response.contentAsString
        return readMap(json)["dependencies"] as List<*>
    }

    private fun suggestionsOf(token: String): List<Map<String, Any?>> {
        val json = mockMvc.perform(get("/api/v1/me/stack/ward-suggestions?limit=50").header("Authorization", "Bearer $token"))
            .andExpect(status().isOk).andReturn().response.contentAsString
        @Suppress("UNCHECKED_CAST")
        return objectMapper.readValue(json, List::class.java) as List<Map<String, Any?>>
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
